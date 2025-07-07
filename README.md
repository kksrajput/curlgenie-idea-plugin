# 🧞‍♂️ cURLGenie

**cURLGenie** is an IntelliJ IDEA plugin that generates `cURL` commands from your **Spring Boot controller methods**.

Just paste a controller method (and optional class definitions), and instantly get a ready-to-run `cURL` command — with support for:

- `@RequestParam`, `@PathVariable`, `@RequestBody`, `@ModelAttribute`, `@RequestHeader`
- JSON / XML body generation
- DTOs, nested classes, interfaces, inheritance
- Map bodies (`Map<String, Object>`)
- Validation annotations
- Header and query param inference

---

## ✨ Features

- 🔍 Detects Spring REST annotations like `@GetMapping`, `@PostMapping`, `@RequestMapping`
- 🧠 Parses DTOs to generate body content (JSON or XML)
- 🧾 Supports nested classes, inheritance, interfaces
- 💡 Smart handling of headers, query/path/body params
- 🧪 JUnit-tested for common and edge cases

---

## 📦 Installation

1. Open IntelliJ IDEA
2. Go to **Settings > Plugins**
3. Click the **Marketplace** tab and search for `cURLGenie`
4. Click **Install**
5. Restart IntelliJ

---

## 🚀 How to Use

1. Open any Java file with a Spring controller method.
2. Go to `Tools > Generate cURL from Spring Method`.
3. Paste your Spring method when prompted.
4. *(Optional)* Paste any class definitions (DTOs) when asked.
5. Click OK – your cURL command is instantly generated!

---

## 🧪 Example Scenarios

### ✅ Simple GET Request

```java
@GetMapping("/hello")
public String sayHello(@RequestParam String name) {}
```

➡️ Generates:

```bash
curl -X GET \
"http://localhost:8080/hello?name=val"
```

---

## 🧾 License

MIT License. See [LICENSE](./LICENSE) for details.

---

## 🤝 Contributing

Feel free to fork this repo, open issues, or create pull requests.

GitHub: [https://github.com/kksrajput/curlgenie-idea-plugin](https://github.com/kksrajput/curlgenie-idea-plugin)

---

## 🙋 FAQ

**Q:** What Spring annotations are supported?  
**A:** `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@RequestMapping`, `@RequestParam`, `@PathVariable`, `@RequestBody`, `@ModelAttribute`, `@RequestHeader`.

**Q:** Can I use this with complex DTOs?  
**A:** Yes! It supports nested classes, inheritance, interfaces, and more.

---

🚀 Enjoy lightning-fast cURL generation with **cURLGenie** inside IntelliJ IDEA!
