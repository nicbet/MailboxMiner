MailboxMiner
============

Tool to mine Mail Repositories in the form of MBOX archives into a PostgreSQL Database
representation. Some of the perks include: 

1. Robustness to different MBOX file formats,
2. Detection of message encoding and transformation into UTF8
3. Automatic and reliable unpacking of MIME messages
4. Deparate saving of attachments
5. Automatic copies of HTML emails into plain text (useful for data mining)
7. Resolution of multiple identities (matches aliases by name heuristics)
8. Recovery of discussion thread structure through multiple heuristics (dates, titles, names, meta-information)

License: GPLv3+
====
MailboxMiner is licensed under the GPLv3 (or any later version).

Usage
====

1. Create a new template database on your PostgreSQL server. A template is provided in MailboxMiner2/dist/new-mm2-template
2. Create a configuration file, see MailboxMiner2/dist/import.sh for an example that you can easily adapt to your settings.
3. Execute import.sh

Reference
====

This tool is closely connected to our research paper titled "An Empirical Study on the Risk of Using Off-the-Shelf Techniques for Processing Mailing List Data"

<b>Abstract</b> - 
<em>Mailing list repositories contain valuable information about the history of a project. Research is starting to mine this information to support developers and maintainers of long-lived software projects. However, such information exists as unstructured data that needs special processing before it can be studied. In this paper, we identify several challenges that arise when using off-the-shelf techniques for processing mailing list data. Our study highlights the importance of proper processing of mailing list data to ensure accurate research results</em>

Download Link: http://nicolas-bettenburg.com/papers/bettenburg-icsm2009.pdf
Copyright © by IEEE. This material is presented to ensure timely dissemination of scholarly and technical work. Copyright and all rights therein are retained by authors or by other copyright holders. All persons copying this information are expected to adhere to the terms and constraints invoked by each author’s copyright. In most cases, these works may not be reposted without the explicit permission of the copyright holder.

BiBTeX Reference:

    @inproceedings{bettenburg:icsm2009,
    author = {Nicolas Bettenburg and Emad Shihab and Ahmed E. Hassan},
    title = {An empirical study on the risks of using off-the-shelf techniques for processing mailing list data},
    booktitle = {ICSM'09: Proceedings of the 25th IEEE International Conference on Software Maintenance},
    year = {2009},
    pages = {539--542},
    publisher = {IEEE Computer Society},
    location = {Edmonton, Alberta}}
