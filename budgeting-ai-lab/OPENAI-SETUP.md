# OpenAI API test setup (manual, no automatic requests)

This branch adds a manually triggered GitHub Actions workflow for one small Budgeting AI test.

## What is and is not connected

- The workflow is prepared to call the OpenAI Responses API using the GitHub repository secret `OPENAI_API_KEY`.
- It is manual-only: it does not run on every push, schedule, or page visit.
- **No API request has been run by creating these files.**
- A run can incur OpenAI API charges, and GitHub Actions may use the repository's Actions allowance.
- The existing HTML demo still does not call OpenAI directly.

## Setup on GitHub (phone-friendly)

1. Review and merge the pull request that adds this workflow into the repository's default branch. GitHub requires a `workflow_dispatch` workflow to exist on the default branch before it can be manually run from the Actions tab.
2. Create an API key at https://platform.openai.com/api-keys. Do not paste it into chat, the HTML file, an issue, or a commit.
3. In GitHub, open the repository **Settings → Secrets and variables → Actions → New repository secret**.
4. Name the secret exactly `OPENAI_API_KEY` and paste the key into the secret-value field. Save it. Never share screenshots that reveal the key.
5. Before running, check your OpenAI API billing and usage limits at https://platform.openai.com/settings/organization/billing/overview and https://platform.openai.com/usage. API calls may be billed separately from ChatGPT.
6. Open **Actions → Budgeting AI API Test → Run workflow**. The default prompt uses fictional budget numbers. Do not run it until you personally approve the possible API charge.

## What happens when manually run

The workflow checks that the secret exists, sends one request to the `gpt-4.1-mini` model, limits the answer to 350 output tokens, and prints the text response in the workflow log. It does not commit changes to your app.

## Security notes

- Keep the repository secret private. GitHub secrets are encrypted, but workflow logs and repository permissions should still be treated carefully.
- Only use fictional sample data in this experiment.
- This is a test workflow, not a production backend for the HTML app. A public HTML page must never contain an API key.
- No paid API request is made until you manually run the workflow.
