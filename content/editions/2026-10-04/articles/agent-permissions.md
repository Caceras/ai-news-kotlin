# Before an AI agent acts, decide what it may do

Useful agents need clear limits. Check the tools, account permissions and approval steps before letting software act on your behalf.

**AI Brief editorial · 4 October 2026 · About 4 minutes**

![A robotic gripper pauses above a brass key on an oak desk beside a green notebook.](../../../media/2026-10-04-agent-permissions.webp)

*AI-generated editorial illustration of an agent and a key; a visual metaphor for permissions, not a real product or event.*

A useful AI assistant might find an overdue invoice, draft a reminder and prepare an email. An agent with broader access could also send the message, change the invoice or delete the original file. Those are different jobs, even if they begin with the same request: “Help me sort this out.”

That is why choosing an agent involves more than comparing the quality of its answers. It also means deciding which actions the software is allowed to take, which accounts it can reach and when it must stop for a decision.

The OWASP Gen AI Security Project calls the risk of giving a model too much room to act “excessive agency.” Its guidance identifies three separate sources of trouble: too many functions, too many permissions and too much autonomy. Each deserves its own check.

## Start with a small, useful job

Imagine a studio that wants an agent to prepare a weekly spending summary. It needs to read a particular folder of receipts and produce a draft report. It does not need permission to alter bank details, pay suppliers or remove records.

A sensible first setup gives it access to that folder and a place to save drafts. The studio can then inspect what the agent actually does before adding another capability. This is an editorial example, rather than a claim about a particular commercial product.

For a reader comparing agent products, the practical question is specific: can the connection grant access to selected files and a limited set of actions? “Connect your account” tells you little about the scope of that connection.

## Enforce the boundary where the action happens

A sentence telling a model to be careful can guide its behaviour. The account or tool it uses must also enforce the boundary. An agent that only needs to read information should use a connection that cannot write or delete it.

OWASP recommends checking authorisation in the systems that perform the action, rather than leaving the model to decide whether its own request is permitted. This matters even when the agent sounds confident and has followed earlier instructions correctly.

It also matters when the agent reads material from outside your control. A webpage, email or document can contain instructions designed to redirect a model. OWASP describes this as indirect prompt injection. The agent may encounter the text while carrying out a legitimate task; the text should not acquire the authority of your original request.

## Make consequential actions visible

For an action such as sending an external message, approving a payment or deleting a file, a useful review screen should show the proposed action and its details. Seeing the recipient, message and attachment is more helpful than approving an unexplained “continue” button.

OWASP recommends human approval for high-impact actions. That does not require approving every harmless step. The point is to match the amount of autonomy to the consequences, while keeping the permission boundary in place.

Activity records and rate limits add another layer. They can help someone spot a problem and reduce how far it spreads, but OWASP distinguishes those measures from controls that prevent excessive agency in the first place.

## A quick check before connecting an account

- Name the job you want the agent to complete and the files it actually needs.
- Check whether reading, writing, sending and deleting are separate permissions.
- Look for a clear preview before consequential actions, plus a way to revoke access.
- Find out where completed actions are recorded and who can review them.

An agent becomes easier to use when its capabilities are understandable. Begin with a bounded task, observe its behaviour and expand access deliberately. The quality of the answer matters; so does the amount of authority behind it.

## Sources & further reading

- [OWASP: LLM06:2025 — Excessive Agency](https://genai.owasp.org/llmrisk/llm062025-excessive-agency/)
- [OWASP: LLM01:2025 — Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/)

*AI-assisted explainer. Claims checked against the linked primary sources on 4 October 2026.*
