import SwiftUI
import UniformTypeIdentifiers
import PDFKit

struct DocumentsView: View {
    @State private var showingImporter = false
    @State private var documentName = ""
    @State private var originalText = ""
    @State private var translatedText = ""
    @State private var sourceLanguage = "English"
    @State private var targetLanguage = "French"
    @State private var isTranslating = false
    @State private var progress = 0
    @State private var errorMessage: String?
    private let translator = TranslationService()

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 12) {
                    Image(systemName: "doc.text.viewfinder").font(.system(size: 52)).foregroundStyle(.blue)
                    Text("Translate a document").font(.title2.bold())
                    Text("Import a PDF, extract its text, translate it, then share or copy the result.")
                        .font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    Button { showingImporter = true } label: {
                        Label("Choose PDF", systemImage: "folder")
                            .frame(maxWidth: .infinity).padding()
                            .foregroundStyle(.white).background(Color.blue.gradient, in: RoundedRectangle(cornerRadius: 16))
                    }
                }.padding(20).frame(maxWidth: .infinity).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))

                if !documentName.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Label(documentName, systemImage: "doc.text").font(.headline).lineLimit(2)
                        HStack {
                            languageMenu("From", selection: $sourceLanguage)
                            Image(systemName: "arrow.right").foregroundStyle(.secondary)
                            languageMenu("To", selection: $targetLanguage)
                        }
                        Text("\(originalText.count) characters extracted")
                            .font(.caption).foregroundStyle(.secondary)
                        Button {
                            Task { await translateDocument() }
                        } label: {
                            HStack {
                                if isTranslating { ProgressView().tint(.white) }
                                Text(isTranslating ? "Translating… \(progress)%" : "Translate document")
                            }.frame(maxWidth: .infinity).padding()
                                .foregroundStyle(.white).background(Color.blue.gradient, in: RoundedRectangle(cornerRadius: 16))
                        }.disabled(isTranslating || originalText.isEmpty)
                    }.padding(16).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22))
                }

                if !translatedText.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("TRANSLATED TEXT").font(.caption.bold()).foregroundStyle(.secondary)
                            Spacer()
                            Button { UIPasteboard.general.string = translatedText } label: {
                                Label("Copy", systemImage: "doc.on.doc")
                            }
                        }
                        Text(translatedText).frame(maxWidth: .infinity, alignment: .leading).textSelection(.enabled)
                        ShareLink(item: translatedText) {
                            Label("Share / export text", systemImage: "square.and.arrow.up")
                                .frame(maxWidth: .infinity).padding()
                                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 14))
                        }
                    }.padding(16).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22))
                }
                if let errorMessage { Text(errorMessage).font(.footnote).foregroundStyle(.red).frame(maxWidth: .infinity, alignment: .leading) }
                Text("PDFs are processed on this device for text extraction. Extracted text is sent to the translation provider when you translate.")
                    .font(.caption).foregroundStyle(.secondary)
            }.padding(.top, 14)
        }
        .fileImporter(isPresented: $showingImporter, allowedContentTypes: [.pdf], allowsMultipleSelection: false) { result in
            switch result {
            case .success(let urls):
                guard let url = urls.first else { return }
                loadPDF(url)
            case .failure(let error):
                errorMessage = error.localizedDescription
            }
        }
    }

    private func languageMenu(_ title: String, selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title.uppercased()).font(.caption2.bold()).foregroundStyle(.secondary)
            Picker(title, selection: selection) {
                Text("English").tag("English")
                Text("French").tag("French")
            }.pickerStyle(.menu)
        }.frame(maxWidth: .infinity, alignment: .leading)
    }

    private func loadPDF(_ url: URL) {
        errorMessage = nil
        translatedText = ""
        let granted = url.startAccessingSecurityScopedResource()
        defer { if granted { url.stopAccessingSecurityScopedResource() } }
        guard let pdf = PDFDocument(url: url) else {
            errorMessage = "This PDF could not be opened."
            return
        }
        let pages = (0..<pdf.pageCount).compactMap { pdf.page(at: $0)?.string }
        let text = pages.joined(separator: "\n\n").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            errorMessage = "No selectable text was found. Scanned PDFs need OCR, which is not included yet."
            return
        }
        documentName = url.lastPathComponent
        originalText = String(text.prefix(12000))
        if text.count > 12000 {
            errorMessage = "This PDF is long. The first 12,000 characters were loaded to keep translation requests manageable."
        }
    }

    @MainActor
    private func translateDocument() async {
        guard !originalText.isEmpty else { return }
        isTranslating = true
        progress = 0
        errorMessage = nil
        translatedText = ""
        defer { isTranslating = false }
        // Split into modest chunks to stay within public translation-service request limits.
        let chunks = splitIntoChunks(originalText, maxLength: 450)
        var output: [String] = []
        for (index, chunk) in chunks.enumerated() {
            do {
                let result = try await translator.translate(chunk, from: sourceLanguage, to: targetLanguage)
                output.append(result)
                progress = Int(Double(index + 1) / Double(chunks.count) * 100)
            } catch {
                errorMessage = "Translation stopped at section \(index + 1): \(error.localizedDescription)"
                translatedText = output.joined(separator: "\n\n")
                return
            }
        }
        translatedText = output.joined(separator: "\n\n")
    }

    private func splitIntoChunks(_ text: String, maxLength: Int) -> [String] {
        var chunks: [String] = []
        var current = ""
        for paragraph in text.components(separatedBy: .newlines) {
            if paragraph.isEmpty { continue }
            if paragraph.count > maxLength {
                if !current.isEmpty { chunks.append(current); current = "" }
                var part = ""
                for word in paragraph.split(separator: " ") {
                    let candidate = part.isEmpty ? String(word) : part + " " + word
                    if candidate.count > maxLength, !part.isEmpty {
                        chunks.append(part)
                        part = String(word)
                    } else { part = candidate }
                }
                if !part.isEmpty { chunks.append(part) }
            } else {
                let candidate = current.isEmpty ? paragraph : current + "\n" + paragraph
                if candidate.count > maxLength, !current.isEmpty {
                    chunks.append(current)
                    current = paragraph
                } else { current = candidate }
            }
        }
        if !current.isEmpty { chunks.append(current) }
        return chunks
    }
}
