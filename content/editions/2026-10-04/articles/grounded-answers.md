# When AI answers from your files, follow the evidence

An employee asks a workplace assistant, “How much notice do I need to give before taking leave?” The answer arrives in seconds, with a link to the company handbook. It looks reassuring. But the link opens last year's handbook, and the relevant paragraph applies only to one office.

This example illustrates an important limit of AI connected to documents. Finding a source is useful. Finding the right source, using the relevant passage and explaining its scope are separate pieces of work.

Retrieval-augmented generation, usually shortened to RAG, connects a language model to material it can retrieve while answering a question. It is commonly used for assistants that search company files, product manuals or other collections. It can make an answer more relevant and easier to inspect, provided the underlying retrieval works well.

## What happens between the question and the answer

In a typical RAG workflow, a search system finds passages related to the question. The application supplies selected passages to the model, which uses that context to compose a response. This is different from retraining the model whenever a document changes.

Google's grounding documentation describes grounding as connecting model output to verifiable sources. It says this can reduce invented content and provide links that support an audit of the response. That is a useful benefit, but it leaves the reader with a job: inspect the evidence behind the answer.

Microsoft's RAG overview sets out several practical challenges. Questions may use different words from the documents. Relevant information may live across several systems. Models accept limited amounts of input, so retrieval has to select concise, useful material. Private files also require access controls.

## Check the passage, date and scope

For the leave-policy question, a strong response would point to the current policy and the passage that applies to the employee's location. A document title alone would not establish either fact.

When an answer matters, open its citation and check three things. First, does the passage actually support the claim? Second, is the document current? Third, do the conditions in the document match the situation in the question?

A source can be real while the answer still overstates what it says. A passage might describe a default rule and then list exceptions. A fluent response can lose those exceptions during summarisation. These are reasons to check the cited text, rather than treating the presence of a link as the end of verification.

## Freshness and access are part of answer quality

A connected assistant depends on the material it can retrieve. For systems built around an index, someone needs to maintain that index as documents are added, changed or retired. Some retrieval designs query remote sources directly; the question to ask remains the same: how does this system obtain the current version?

Access should be just as explicit. Microsoft notes that users and agents must retrieve only content they are authorised to see. A search layer should respect the requesting user's permissions. A polished answer is not a reason to expose a confidential file to someone who could not otherwise open it.

Retrieved material can also contain malicious instructions. OWASP warns that RAG does not fully resolve prompt-injection vulnerabilities. A document should supply evidence for the answer, without being allowed to redefine the assistant's task or its permissions.

## Put a document assistant through useful trials

- Ask a question with a clear answer in a current document and inspect the cited passage.
- Ask about an exception, an outdated policy and a topic the collection does not cover.
- Use an account without access to a restricted file and check that the file stays restricted.
- Look for a clear acknowledgement when the available evidence is insufficient.

These are suggested evaluation tasks, not results from a test of a particular product. They help reveal whether the assistant can handle uncertainty as well as an easy question. A good document answer gives you a shorter route to the relevant evidence and makes that evidence easy to check.
