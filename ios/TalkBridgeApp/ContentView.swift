import SwiftUI
import PhotosUI
import UIKit

private enum AppSection: String, CaseIterable, Identifiable {
    case translator = "Translator"
    case conversation = "Conversation"
    case documents = "Documents"
    case settings = "Settings"
    var id: String { rawValue }
    var symbol: String {
        switch self {
        case .translator: return "character.bubble"
        case .conversation: return "waveform"
        case .documents: return "doc.text"
        case .settings: return "slider.horizontal.3"
        }
    }
}

struct ContentView: View {
    @AppStorage("tb_darkMode") private var darkMode = false
    @AppStorage("tb_fontScale") private var fontScale = 1.0
    @AppStorage("tb_autoTranslate") private var autoTranslate = true
    @AppStorage("tb_wallpaperData") private var wallpaperData = Data()
    @State private var section: AppSection = .translator
    @State private var sourceLanguage = "English"
    @State private var targetLanguage = "French"
    @State private var input = ""
    @State private var translated = ""
    @State private var isTranslating = false
    @State private var errorMessage: String?
    @State private var photoItem: PhotosPickerItem?
    @State private var showMenu = false
    private let languages = ["English", "French"]
    private let translator = TranslationService()

    private var customWallpaper: UIImage? { wallpaperData.isEmpty ? nil : UIImage(data: wallpaperData) }

    var body: some View {
        ZStack {
            background
            NavigationStack {
                Group {
                    switch section {
                    case .translator: translatorPage
                    case .conversation: ConversationView()
                    case .documents: DocumentsView()
                    case .settings: settingsPage
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 12)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                .navigationTitle("TalkBridge")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button { showMenu = true } label: {
                            Image(systemName: "line.3.horizontal").font(.system(size: 17, weight: .semibold))
                        }.accessibilityLabel("Open navigation")
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Image(systemName: "waveform").foregroundStyle(.blue)
                    }
                }
                .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
                .sheet(isPresented: $showMenu) { navigationSheet }
            }
        }
        .preferredColorScheme(darkMode ? .dark : .light)
        .task(id: input) {
            guard autoTranslate, !input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                translated = ""
                return
            }
            try? await Task.sleep(for: .milliseconds(250))
            guard !Task.isCancelled else { return }
            await translateText()
        }
        .onChange(of: photoItem) { _, item in
            Task {
                guard let data = try? await item?.loadTransferable(type: Data.self) else { return }
                wallpaperData = data
            }
        }
    }

    private var background: some View {
        ZStack {
            Color(uiColor: .systemGroupedBackground).ignoresSafeArea()
            if let image = customWallpaper {
                Image(uiImage: image).resizable().scaledToFill()
                    .overlay(.ultraThinMaterial.opacity(0.35)).ignoresSafeArea()
            } else {
                LinearGradient(colors: [Color.blue.opacity(0.10), Color.purple.opacity(0.06), .clear],
                               startPoint: .topLeading, endPoint: .bottomTrailing).ignoresSafeArea()
            }
        }
    }

    private var translatorPage: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack(spacing: 8) {
                    languagePicker(title: "FROM", selection: $sourceLanguage)
                    Button {
                        let oldSource = sourceLanguage
                        sourceLanguage = targetLanguage
                        targetLanguage = oldSource
                        input = translated
                        translated = ""
                    } label: {
                        Image(systemName: "arrow.left.arrow.right")
                            .frame(width: 42, height: 42).background(.regularMaterial, in: Circle())
                    }.accessibilityLabel("Swap languages")
                    languagePicker(title: "TO", selection: $targetLanguage)
                }.padding(12).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22))

                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text(sourceLanguage.uppercased()).font(.caption.weight(.bold)).foregroundStyle(.secondary)
                        Spacer()
                        Button { input = ""; translated = ""; errorMessage = nil } label: {
                            Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
                        }
                    }
                    TextEditor(text: $input)
                        .font(.system(size: 18 * fontScale))
                        .scrollContentBackground(.hidden).frame(minHeight: 145)
                        .accessibilityLabel("Text to translate")
                    HStack {
                        Text("\(input.count) characters").font(.caption).foregroundStyle(.secondary)
                        Spacer()
                        Button { Task { await translateText() } } label: {
                            HStack {
                                if isTranslating { ProgressView().tint(.white) }
                                Text(isTranslating ? "Translating…" : "Translate")
                            }
                            .font(.system(size: 15, weight: .semibold))
                            .padding(.horizontal, 18).padding(.vertical, 11)
                            .foregroundStyle(.white).background(Color.blue.gradient, in: Capsule())
                        }
                        .disabled(input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isTranslating)
                    }
                }.padding(16).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))

                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text(targetLanguage.uppercased()).font(.caption.weight(.bold)).foregroundStyle(.secondary)
                        Spacer()
                        Button { UIPasteboard.general.string = translated } label: {
                            Label("Copy", systemImage: "doc.on.doc").font(.caption.weight(.semibold))
                        }.disabled(translated.isEmpty)
                    }
                    Text(translated.isEmpty ? "Your translation will appear here." : translated)
                        .font(.system(size: 18 * fontScale))
                        .foregroundStyle(translated.isEmpty ? .secondary : .primary)
                        .frame(maxWidth: .infinity, minHeight: 105, alignment: .topLeading)
                        .textSelection(.enabled)
                    if let errorMessage { Text(errorMessage).font(.footnote).foregroundStyle(.red) }
                }.padding(16).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))
                Text("English ↔ French").font(.caption.weight(.medium)).foregroundStyle(.secondary)
            }.padding(.top, 12)
        }
    }

    private func languagePicker(title: String, selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title).font(.caption2.weight(.bold)).foregroundStyle(.secondary)
            Picker(title, selection: selection) {
                ForEach(languages, id: \.self) { Text($0).tag($0) }
            }.pickerStyle(.menu).font(.system(size: 15, weight: .semibold))
        }.frame(maxWidth: .infinity, alignment: .leading)
    }

    private var conversationPage: some View {
        VStack(spacing: 16) {
            Image(systemName: "waveform.circle.fill").font(.system(size: 56)).foregroundStyle(.blue)
            Text("Conversation mode").font(.title2.bold())
            Text("Live microphone and two-way spoken translation are planned for the next stage.")
                .multilineTextAlignment(.center).foregroundStyle(.secondary)
            Button { section = .translator } label: {
                Label("Open translator", systemImage: "character.bubble")
                    .frame(maxWidth: .infinity).padding()
                    .background(Color.blue.gradient, in: RoundedRectangle(cornerRadius: 16))
                    .foregroundStyle(.white).fontWeight(.semibold)
            }
            Spacer()
        }.padding(.top, 48)
    }

    private var documentsPage: some View {
        VStack(spacing: 16) {
            Image(systemName: "doc.text.viewfinder").font(.system(size: 54)).foregroundStyle(.blue)
            Text("Document translation").font(.title2.bold())
            Text("PDF import, full-document translation, and export are planned for a future build.")
                .multilineTextAlignment(.center).foregroundStyle(.secondary)
            Spacer()
        }.padding(.top, 48)
    }

    private var settingsPage: some View {
        Form {
            Section("Appearance") {
                Toggle("Dark appearance", isOn: $darkMode)
                PhotosPicker(selection: $photoItem, matching: .images) {
                    Label("Choose wallpaper photo", systemImage: "photo")
                }
                if !wallpaperData.isEmpty {
                    Button(role: .destructive) { wallpaperData = Data() } label: {
                        Label("Remove wallpaper", systemImage: "trash")
                    }
                }
                HStack {
                    Text("Text size")
                    Slider(value: $fontScale, in: 0.85...1.3, step: 0.05)
                    Text("\(Int(fontScale * 100))%").monospacedDigit().foregroundStyle(.secondary)
                }
            }
            Section("Translation") {
                Toggle("Translate as I type", isOn: $autoTranslate)
                LabeledContent("Languages", value: "English ↔ French")
                Text("Online translation requires a secure service endpoint. Keep API credentials on the server.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
            Section {
                Label("Preferences save automatically on this device.", systemImage: "checkmark.circle")
                    .foregroundStyle(.secondary)
            }
        }.scrollContentBackground(.hidden)
    }

    private var navigationSheet: some View {
        NavigationStack {
            List {
                ForEach(AppSection.allCases) { item in
                    Button {
                        section = item
                        showMenu = false
                    } label: {
                        Label(item.rawValue, systemImage: item.symbol)
                            .foregroundStyle(section == item ? Color.blue : Color.primary)
                    }
                }
            }
            .navigationTitle("TalkBridge")
            .navigationBarTitleDisplayMode(.inline)
            .presentationDetents([.medium, .large])
        }
    }

    @MainActor
    private func translateText() async {
        let textToTranslate = input
        guard !textToTranslate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        isTranslating = true
        defer { isTranslating = false }
        do {
            translated = try await translator.translate(textToTranslate, from: sourceLanguage, to: targetLanguage)
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
