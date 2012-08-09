MailboxMiner
============

Tool to mine Mail Repositories in the form of MBOX archives into a PostgreSQL Database representation. Some of the perks include: robustness to different MBOX file formats, detection of message encoding and transformation into UTF8, unpacking of MIME messages, separate saving of attachments, automatic copies of HTML emails into plain text (useful for data mining), resolution of multiple identities (matches aliases by name heuristics), and many more.