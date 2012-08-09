MailboxMiner
============

Tool to mine Mail Repositories in the form of MBOX archives into a PostgreSQL Database
representation. Some of the perks include: robustness to different MBOX file formats,
detection of message encoding and transformation into UTF8, unpacking of MIME
messages, separate saving of attachments, automatic copies of HTML emails into plain
text (useful for data mining), resolution of multiple identities (matches aliases by
name heuristics), and many more.

Usage
====

1. Create a new template database on your PostgreSQL server. A template is provided in MailboxMiner2/dist/new-mm2-template
2. Create a configuration file, see MailboxMiner2/dist/import.sh for an example that you can easily adapt to your settings.
3. Execute import.sh