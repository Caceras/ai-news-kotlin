import importlib.util
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


if __name__ == '__main__':
    unittest.main()
