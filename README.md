# HookShot

HookShot is an HTTP request dispatcher for local and remote networks.  
It allows you to send configurable HTTP requests with custom headers, multiple bodies, and different HTTP methods — all from a simple graphical interface.
<img width="1920" height="1080" alt="hookshot_mainmenu_censored" src="https://github.com/user-attachments/assets/28a2c72d-1775-4e16-861d-2725e482ec5f" />
<img width="1548" height="896" alt="Screenshot_hookshot2" src="https://github.com/user-attachments/assets/8292e70a-4910-4824-9062-e5762acbf1da" />

---

## Features

- **Multiple HTTP methods** — GET, POST, PUT, PATCH, DELETE
- **Dynamic headers** — add and remove headers at runtime
- **Multiple bodies** — separate bodies with a double newline and send them in configurable parallel batches
- **JSON pretty-print** — responses are automatically formatted if the body is valid JSON
- **Packet templates** — save and load full request configurations (method, URL, headers, body)
- **Header templates** — save and load header sets independently
- **URL templates** — save and load URLs independently
- **Log system** — all responses are accumulated in a log panel and can be saved to a `.txt` file
- **Theme system** — load custom CSS themes from the user config directory without recompiling
- **Multi-language support** — English, Italian, French, Spanish, German, Polish
- **HTTP version selector** — switch between HTTP/1.1 and HTTP/2
- **Persistent settings** — language, max parallel requests, HTTP version, last used URL and current session are saved across restarts

---

## Requirements

- Java 21 or later
- Maven 3.8 or later (for building from source)

---

## Installation

### Run from source

```bash
git clone https://github.com/CookieMaker443/HookShot.git
cd HookShot/dispatcher
mvn clean javafx:run
```

### Build a JAR

```bash
mvn clean package
```

The JAR will be available in `dispatcher/target/`.

> Pre-built releases with installers for Linux and Windows will be available soon.

---

## Configuration files

On first launch, HookShot creates its configuration directory automatically:

| Platform | Path |
|---|---|
| Linux | `~/.config/hookshot/` |
| Windows | `%APPDATA%\hookshot\` |

The directory structure is:

```
hookshot/
├── settings.properties          — app settings
├── themes/
│   └── default.css              — active CSS theme (editable)
├── saved_packet_templates/      — saved request packets
├── saved_headers_templates/     — saved header sets
├── saved_url_templates/         — saved URLs
└── saved_logs/                  — default location for saved logs
```

---

## Themes

HookShot supports custom CSS themes. On first launch, `default.css` is copied to the `themes/` folder in the config directory.

To create a custom theme:
1. Copy `default.css` and rename it (e.g. `dark.css`)
2. Edit the file — JavaFX CSS properties use the `-fx-` prefix
3. Open HookShot settings and select the new theme from the dropdown
4. Save — the theme is applied on the next scene reload

If a theme file is deleted or missing, HookShot automatically falls back to the built-in default theme.

### Theme variables

Colors are defined as named variables on `.root` and reused throughout the file:

```css
.root {
    background-dark: #1e1e1e;
    accent-color: #2d7dd2;
    text-primary: #d4d4d4;
}
```

---

## Packet format

Packet files are plain text with a simple key=value format:

```
METHOD=POST
URL=https://example.com/api/endpoint
HEADER=Content-Type : application/json
HEADER=Authorization : Bearer token123
BODY={"key": "value"}
```

Multiple bodies for batch sending are separated by a double newline in the body field.

---

## Multiple body / batch sending

Write multiple JSON bodies in the body area, separated by a blank line:

```
{"name": "Mario"}

{"name": "Luigi"}

{"name": "Peach"}
```

HookShot will split them and send them in parallel batches. The batch size is configurable in settings (default: 3).

---

## Settings

| Setting | Description | Default |
|---|---|---|
| Language | UI language | English |
| Max parallel requests | Max requests per batch | 3 |
| HTTP Version | HTTP/1.1 or HTTP/2 | HTTP/1.1 |
| Theme | CSS theme file from themes folder | default.css |

> **Note:** If your server does not respond when using HTTP/2, switch to HTTP/1.1. Some servers (e.g. n8n webhooks) do not handle HTTP/2 correctly.

---

## Roadmap

- [ ] Pre-built installers for Linux (AppImage, .tar.gz) and Windows (.exe)
- [ ] GitHub Actions release workflow
- [ ] Auto-update check on startup
- [ ] App icon
- [X] Screenshots in README

---

## License

This project is licensed under the MIT License.

```
MIT License

Copyright (c) 2026 CookieMaker443

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Author

**CookieMaker443**  
[github.com/CookieMaker443](https://github.com/CookieMaker443)
