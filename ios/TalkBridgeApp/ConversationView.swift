import SwiftUI
import Speech
import AVFoundation

struct ConversationView: View {
    @State private var sourceLanguage = "English"
    @State private var targetLanguage = "French"
    @State private var recognizedText = ""
    @State private var translatedText = ""
    @State private var status = "Tap the microphone and speak."
    @State private var listening = false
    @State private var busy = false
    @State private var errorMessage: String?
    @State private var recognizer = SpeechConversationController()
    private let translator = TranslationService()
    private let speech = AVSpeechSynthesizer()

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                HStack {
                    languageMenu("Speak", selection: $sourceLanguage)
                    Button {
                        let old = sourceLanguage
                        sourceLanguage = targetLanguage
                        targetLanguage = old
                        recognizedText = ""
                        translatedText = ""
                    } label: {
                        Image(systemName: "arrow.left.arrow.right")
                            .frame(width: 42, height: 42).background(.regularMaterial, in: Circle())
                    }
                    languageMenu("Translate to", selection: $targetLanguage)
                }
                .padding(12).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))

                VStack(spacing: 14) {
                    Image(systemName: listening ? "waveform.circle.fill" : "waveform.circle")
                        .font(.system(size: 68)).foregroundStyle(listening ? .red : .blue)
                    Text(status).font(.headline).multilineTextAlignment(.center)
                    Button {
                        if listening { stopListening() } else { Task { await startListening() } }
                    } label: {
                        Label(listening ? "Stop listening" : "Start speaking",
                              systemImage: listening ? "stop.fill" : "mic.fill")
                            .frame(maxWidth: .infinity).padding()
                            .foregroundStyle(.white)
                            .background(listening ? Color.red.gradient : Color.blue.gradient,
                                        in: RoundedRectangle(cornerRadius: 16))
                    }.disabled(busy)
                    Text("Speech recognition requires microphone and speech permissions.")
                        .font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                .padding(20).frame(maxWidth: .infinity)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))

                VStack(alignment: .leading, spacing: 8) {
                    Text("WHAT YOU SAID").font(.caption.bold()).foregroundStyle(.secondary)
                    Text(recognizedText.isEmpty ? "Your speech will appear here." : recognizedText)
                        .frame(maxWidth: .infinity, minHeight: 70, alignment: .topLeading)
                        .textSelection(.enabled)
                }.padding(16).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))

                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("TRANSLATION").font(.caption.bold()).foregroundStyle(.secondary)
                        Spacer()
                        Button { speakTranslation() } label: {
                            Label("Speak", systemImage: "speaker.wave.2.fill")
                        }.disabled(translatedText.isEmpty)
                    }
                    Text(translatedText.isEmpty ? "The translation will appear here." : translatedText)
                        .frame(maxWidth: .infinity, minHeight: 70, alignment: .topLeading)
                        .textSelection(.enabled)
                    if let errorMessage { Text(errorMessage).font(.footnote).foregroundStyle(.red) }
                }.padding(16).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
            }.padding(.top, 14)
        }
        .onDisappear { stopListening() }
    }

    private func languageMenu(_ title: String, selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title.uppercased()).font(.caption2.bold()).foregroundStyle(.secondary)
            Picker(title, selection: selection) {
                Text("English").tag("English")
                Text("French").tag("French")
            }.pickerStyle(.menu).font(.subheadline.bold())
        }.frame(maxWidth: .infinity, alignment: .leading)
    }

    @MainActor
    private func startListening() async {
        errorMessage = nil
        do {
            let text = try await recognizer.start(language: sourceLanguage)
            listening = true
            status = "Listening… speak clearly."
            // Speech controller publishes live text through its callback.
            recognizer.onText = { value in
                Task { @MainActor in
                    recognizedText = value
                    await translateRecognizedText()
                }
            }
            _ = text
        } catch {
            errorMessage = error.localizedDescription
            status = "Could not start listening."
        }
    }

    @MainActor
    private func stopListening() {
        recognizer.stop()
        listening = false
        status = recognizedText.isEmpty ? "Tap the microphone and speak." : "Stopped listening."
    }

    @MainActor
    private func translateRecognizedText() async {
        let value = recognizedText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty, !busy else { return }
        busy = true
        defer { busy = false }
        do {
            translatedText = try await translator.translate(value, from: sourceLanguage, to: targetLanguage)
            errorMessage = nil
        } catch { errorMessage = error.localizedDescription }
    }

    private func speakTranslation() {
        guard !translatedText.isEmpty else { return }
        let utterance = AVSpeechUtterance(string: translatedText)
        utterance.voice = AVSpeechSynthesisVoice(language: targetLanguage == "French" ? "fr-FR" : "en-US")
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        speech.speak(utterance)
    }
}

@MainActor
final class SpeechConversationController: ObservableObject {
    private let audioEngine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    var onText: ((String) -> Void)?

    func start(language: String) async throws -> String {
        let speechStatus = await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { continuation.resume(returning: $0) }
        }
        guard speechStatus == .authorized else { throw SpeechConversationError.speechPermission }
        let audioGranted = await withCheckedContinuation { continuation in
            AVAudioApplication.requestRecordPermission { continuation.resume(returning: $0) }
        }
        guard audioGranted else { throw SpeechConversationError.microphonePermission }
        guard let speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: language == "French" ? "fr-FR" : "en-US")),
              speechRecognizer.isAvailable else { throw SpeechConversationError.unavailable }

        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.playAndRecord, mode: .measurement, options: [.defaultToSpeaker, .allowBluetooth])
        try audioSession.setActive(true)
        request = SFSpeechAudioBufferRecognitionRequest()
        guard let request else { throw SpeechConversationError.unavailable }
        request.shouldReportPartialResults = true
        let input = audioEngine.inputNode
        let format = input.outputFormat(forBus: 0)
        input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in request.append(buffer) }
        audioEngine.prepare()
        try audioEngine.start()
        task = speechRecognizer.recognitionTask(with: request) { [weak self] result, error in
            if let result { self?.onText?(result.bestTranscription.formattedString) }
            if error != nil { Task { @MainActor in self?.stop() } }
        }
        return ""
    }

    func stop() {
        if audioEngine.isRunning { audioEngine.stop() }
        audioEngine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        task?.cancel()
        request = nil
        task = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

enum SpeechConversationError: LocalizedError {
    case speechPermission, microphonePermission, unavailable
    var errorDescription: String? {
        switch self {
        case .speechPermission: return "Please allow Speech Recognition in iPhone Settings for TalkBridge."
        case .microphonePermission: return "Please allow microphone access in iPhone Settings for TalkBridge."
        case .unavailable: return "Speech recognition is unavailable for this language or device right now."
        }
    }
}
