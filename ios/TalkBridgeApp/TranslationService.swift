import Foundation

struct TranslationService {
    // Configure a secure server endpoint here at build time. Never embed Google API keys in the app.
    func translate(_ text: String, from source: String, to target: String) async throws -> String {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "" }
        guard let endpointString = Bundle.main.object(forInfoDictionaryKey: "TALKBRIDGE_TRANSLATION_ENDPOINT") as? String,
              !endpointString.isEmpty, let endpoint = URL(string: endpointString) else {
            throw TranslationError.notConfigured
        }
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "text": trimmed, "source": source.lowercased(), "target": target.lowercased()
        ])
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw TranslationError.server
        }
        let object = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let result = object?["translatedText"] as? String, !result.isEmpty else {
            throw TranslationError.invalidResponse
        }
        return result
    }
}

enum TranslationError: LocalizedError {
    case notConfigured, server, invalidResponse
    var errorDescription: String? {
        switch self {
        case .notConfigured: return "Online translation is not connected yet. A secure translation endpoint must be configured."
        case .server: return "The translation service is unavailable. Please try again."
        case .invalidResponse: return "The translation service returned an unexpected response."
        }
    }
}
