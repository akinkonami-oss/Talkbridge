import Foundation

struct TranslationService {
    // Prefer a private HTTPS backend configured via TALKBRIDGE_TRANSLATION_ENDPOINT.
    // When none is configured, use MyMemory's public translation API as a no-key prototype fallback.
    // Do not send confidential, medical, legal, or otherwise sensitive text through a public endpoint.
    func translate(_ text: String, from source: String, to target: String) async throws -> String {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "" }

        if let endpointString = Bundle.main.object(forInfoDictionaryKey: "TALKBRIDGE_TRANSLATION_ENDPOINT") as? String,
           !endpointString.isEmpty, let endpoint = URL(string: endpointString) {
            var request = URLRequest(url: endpoint)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.timeoutInterval = 25
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

        // Public prototype fallback. The provider may rate-limit requests and is not a production SLA.
        var components = URLComponents(string: "https://api.mymemory.translated.net/get")
        components?.queryItems = [
            URLQueryItem(name: "q", value: String(trimmed.prefix(450))),
            URLQueryItem(name: "langpair", value: "\(languageCode(source))|\(languageCode(target))")
        ]
        guard let url = components?.url else { throw TranslationError.server }
        var request = URLRequest(url: url)
        request.timeoutInterval = 25
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw TranslationError.server
        }
        let object = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        if let responseStatus = object?["responseStatus"] as? Int, responseStatus != 200 {
            throw TranslationError.server
        }
        let responseData = object?["responseData"] as? [String: Any]
        guard let result = responseData?["translatedText"] as? String, !result.isEmpty else {
            throw TranslationError.invalidResponse
        }
        return result
    }

    private func languageCode(_ language: String) -> String {
        switch language.lowercased() {
        case "english": return "en"
        case "french": return "fr"
        default: return language.lowercased()
        }
    }
}

enum TranslationError: LocalizedError {
    case notConfigured, server, invalidResponse
    var errorDescription: String? {
        switch self {
        case .notConfigured: return "Online translation is not connected yet. Configure a secure translation endpoint."
        case .server: return "The translation service is unavailable or has reached its usage limit. Please try again."
        case .invalidResponse: return "The translation service returned an unexpected response."
        }
    }
}
