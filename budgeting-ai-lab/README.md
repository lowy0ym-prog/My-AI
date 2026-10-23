# Budgeting AI Lab (safe demo)

A mobile-friendly, single-file budgeting demo for testing the GitHub workflow. It uses fictional sample data and a simple in-browser rules engine.

## Safety and cost

- This version makes **no OpenAI API requests**.
- It does not require an API key, payment method, server, build tools, or dependencies.
- Do not paste an API key into `index.html` or commit secrets.
- The report is a demo analysis, not output from an OpenAI model.

## Try it on your phone

1. Open [the app file](https://github.com/lowy0ym-prog/My-AI/blob/test/budgeting-ai-demo/budgeting-ai-lab/index.html).
2. On GitHub, tap **Raw** to view the HTML source, or use the browser's download option if available.
3. Easiest option: enable GitHub Pages for this branch/repository only if you want a hosted web page. Because this is a test branch in an Android project repository, a separate repository for Pages may be cleaner.
4. Edit income and expenses, then tap **Analyze my budget**.

## Next step: optional OpenAI integration

The next version could call OpenAI through a small backend or a manually triggered GitHub Actions workflow. Never call the OpenAI API directly from a public HTML page because that exposes the secret key. Before enabling API requests, configure a usage limit and billing controls, and explicitly approve a test request. No API workflow is included or enabled in this demo.
