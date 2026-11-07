import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('publisher', Path(__file__).with_name('publish-post.py'))
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class PublishingTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        publisher.POSTS = Path(self.directory.name) / 'posts.json'
        self.initial = Path(__file__).resolve().parents[1].joinpath('content/posts.json').read_bytes()
        publisher.POSTS.write_bytes(self.initial)
        self.draft = {'title': 'A useful briefing', 'summary': 'What changed and why it matters.',
            'author': 'AI Brief', 'category': 'RESEARCH',
            'body': 'Original reporting and context. ' * 8,
            'publishedAt': '2026-10-04T09:00:00+02:00',
            'sources': [{'text': 'Primary source', 'url': 'https://example.com/source'}]}

    def test_publication_preserves_edition_and_converts_schedule_to_utc(self):
        publisher.publish(self.draft)
        after = json.loads(publisher.POSTS.read_text())
        before = json.loads(self.initial)
        self.assertEqual(before['posts'], after['posts'][1:])
        self.assertEqual('2026-10-04T07:00:00Z', after['posts'][0]['publishedAt'])
        self.assertEqual('link', after['posts'][0]['blocks'][-1]['type'])

    def test_invalid_source_never_changes_the_live_edition(self):
        self.draft['sources'][0]['url'] = 'javascript:alert(1)'
        with self.assertRaises(ValueError):
            publisher.publish(self.draft)
        self.assertEqual(self.initial, publisher.POSTS.read_bytes())

    def test_duplicate_submission_is_rejected_without_a_partial_write(self):
        publisher.publish(self.draft)
        published = publisher.POSTS.read_bytes()
        with self.assertRaises(ValueError):
            publisher.publish(self.draft)
        self.assertEqual(published, publisher.POSTS.read_bytes())

    def test_prose_is_data_and_survives_quotes_unicode_and_shell_characters(self):
        title = 'Quoted “AI” with `backticks` and $(commands)'
        self.draft['title'] = title
        self.draft['body'] += '\n\nA quote: "hello". A path: \\example. Unicode: åäö.'
        publisher.publish(self.draft)
        post = json.loads(publisher.POSTS.read_text())['posts'][0]
        self.assertEqual(title, post['title'])
        self.assertEqual('A quote: "hello". A path: \\example. Unicode: åäö.', post['blocks'][1]['text'])

    def test_a_revision_keeps_the_id_and_dates_the_update(self):
        from datetime import datetime, timezone
        self.draft['sourceIssue'] = 7
        identifier, outcome = publisher.publish(self.draft)
        self.assertEqual('published', outcome)
        self.draft['title'] = 'A sharper headline'
        later = datetime(2026, 10, 5, tzinfo=timezone.utc)
        again, outcome = publisher.publish(self.draft, now=later)
        self.assertEqual((identifier, 'updated'), (again, outcome))
        posts = json.loads(publisher.POSTS.read_text())['posts']
        post = next(p for p in posts if p['id'] == identifier)
        self.assertEqual('A sharper headline', post['title'])
        self.assertEqual('2026-10-05T00:00:00Z', post['updatedAt'])
        self.assertEqual(len(json.loads(self.initial)['posts']) + 1, len(posts))

    def test_unpublish_removes_only_that_story(self):
        self.draft['sourceIssue'] = 9
        identifier, _ = publisher.publish(self.draft)
        self.assertEqual(identifier, publisher.unpublish(source_issue=9))
        self.assertEqual(json.loads(self.initial), json.loads(publisher.POSTS.read_text()))
        with self.assertRaises(ValueError):
            publisher.unpublish(source_issue=9)

    def test_format_rules_are_enforced(self):
        for change, reason in [
            ({'format': 'SPONSORED'}, 'sponsored without a sponsor'),
            ({'format': 'NEWS', 'sponsor': 'Acme'}, 'a sponsor on news'),
            ({'format': 'OPINION'}, 'an unsigned column'),
            ({'format': 'HOLOGRAM'}, 'an unknown type'),
            ({'kicker': 'A kicker that is far too long to read'}, 'a long kicker'),
            ({'title': 'x' * 121}, 'a long headline'),
            ({'theme': 'NEON'}, 'an unknown card colour'),
            ({'summary': 'Demo story to preview the design. Not real news.'}, 'a placeholder story'),
        ]:
            draft = dict(self.draft, author='AI Brief editorial', **change)
            with self.assertRaises(ValueError, msg=reason):
                publisher.publish(draft)
        self.assertEqual(self.initial, publisher.POSTS.read_bytes())
        publisher.publish(dict(self.draft, format='SPONSORED', sponsor='Acme', theme='MINT'))

    def test_a_picture_alone_on_a_line_becomes_an_image_block(self):
        blocks = publisher.body_blocks('Intro paragraph.\n\n![image](https://e.com/a.webp)\n\n![Chart of scores](https://e.com/b.webp)')
        self.assertEqual({'type': 'image', 'url': 'https://e.com/a.webp'}, blocks[1])
        self.assertEqual('Chart of scores', blocks[2]['caption'])


class IssueFormTest(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location('issue_post', Path(__file__).with_name('issue_post.py'))
        self.issue = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.issue)
        self.body = """### Action\n\nPublish or update\n\n### Story type\n\nOpinion (signed column with portrait)\n\n### Topic\n\nPOLICY\n\n### Headline\n\nWe grade AI on the wrong tests\n\n### Kicker\n\n_No response_\n\n### Deck\n\nBenchmarks reward the wrong things.\n\n### Pictures\n\n_No response_\n\n### Author\n\nRiki Caceras\n\n### Author role\n\nColumnist\n\n### Author photo\n\n_No response_\n\n### Story\n\nFirst paragraph.\n\n## A heading\n\nSecond.\n\n### Sources\n\nNIST | https://www.nist.gov/\nhttps://example.com/b\n\n### Card colour\n\n_No response_\n\n### Sponsor\n\n_No response_\n\n### Publish at\n\n_No response_"""

    def test_the_form_becomes_a_draft(self):
        draft = self.issue.draft_from_issue(self.body, 12, None)
        self.assertEqual('OPINION', draft['format'])
        self.assertEqual('POLICY', draft['category'])
        self.assertEqual('Riki Caceras', draft['author'])
        self.assertEqual('Columnist', draft['authorRole'])
        self.assertIsNone(draft['kicker'])
        self.assertEqual(12, draft['sourceIssue'])
        self.assertEqual([{'text': 'NIST', 'url': 'https://www.nist.gov/'},
                          {'text': None, 'url': 'https://example.com/b'}], draft['sources'])

    def test_pasted_pictures_are_found_in_every_form(self):
        value = '![image](https://github.com/user-attachments/assets/a)\n<img width="300" src="https://github.com/user-attachments/assets/b" />\nhttps://e.com/c.jpg'
        self.assertEqual(['https://github.com/user-attachments/assets/a', 'https://github.com/user-attachments/assets/b',
                          'https://e.com/c.jpg'], self.issue.picture_urls(value))

    def picture(self, colour):
        from PIL import Image
        out = io.BytesIO()
        Image.new('RGB', (40, 20), colour).save(out, 'PNG')
        return out.getvalue()

    def test_a_replaced_picture_gets_a_new_address_and_the_old_file_goes(self):
        with tempfile.TemporaryDirectory() as folder:
            self.issue.MEDIA = Path(folder)
            other = Path(folder, 'issue-120-1-keep.webp')
            other.write_bytes(b'x')
            self.issue.fetch = lambda url, token: self.picture('red')
            first = self.issue.save_picture('https://e.com/a', 'issue-12-1', None)
            self.issue.fetch = lambda url, token: self.picture('blue')
            second = self.issue.save_picture('https://e.com/b', 'issue-12-1', None)
            self.assertNotEqual(first, second)
            self.issue.remove_unused_media(12, {'imageUrl': second})
            self.assertEqual([second.rsplit('/', 1)[1]], [p.name for p in self.issue.issue_media(12)])
            self.issue.remove_unused_media(12)
            self.assertEqual([], self.issue.issue_media(12))
            self.assertTrue(other.exists())


if __name__ == '__main__':
    unittest.main()
