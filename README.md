# ElectionCalc

In text - "pesel" is a polish "Personal ID number"
/** What was not implemented according to task requirements:
- GUI - program is written as a text and no GUI for now - this topic is "to be learned" in some weeks
- Multithreading - program is not protected by using in several instances in the same time - this topic is "to be learned"
- Protect DB from external attacks - only one protection which is made is a "preparedStatements" whcih protects DB from 
  SQL Injection attacks
- Datas are stored in DB but aren't coded/hashed. Only pesel is hashed in quite easy way using modified Cesars code with
  a static key. Whis method is valid and safe for pesel considering pesel length and ascii values for digits. In case of
  longer Strings problems may appear. Security of data is one of "to be learned" topics.
- There is no PDF export. Export is possible only for CSV files.
- Program is designed to work with DB that's why only composition (candidate -> party) is used 
  if we consider OOP paradigmates. Candidate and Party class have got @Overrided equals() and hashcode() methods
  to work properly with used HashSet
*/

Please set your Java version to the 11.0.1 (11), otherwise some methods may not work properly.
To work properly it's need to implement sqlite-jdbc-3.27.2.1 library (included in project). 
It's already included in exported JAR file
To work with this application you need to be connected to the internet.
Directory of electData.db should be in main folder.

  Calculator is design, every time user restart application all tables except "voters" will be restarted so bear in mind
to export datas to CSV file while quitting program.
  Tables "blockedPers", "candidates", "partys" are updated automatically.

Database contains tables:
- blockedPers (_id INTEGER, pesel TEXT)
- candidates  (_id INTEGER, name  TEXT, partyId INTEGER, votes INTEGER)
- partys      (_id INTEGER, name  TEXT, votes INTEGER)
- voters      (_id INTEGER, name  TEXT, pesel TEXT, voted INTEGER (boolean))
- valVotes    (status TEXT, number INTEGER)

*blockedPers - get, and store coded "pesel" numbers of people which no voting rights. Every time user try to acces to vote,
            program compare his "pesel" with this table,
*candidates - get and store imported candidates. Contain partyId field and number of votes taken,
*partys     - get and store imported parties. Contain field and number of votes taken,
*voters     - get and store information about users which have already voted,
*valVotes   - get and store information about validate/nonvalidate/nopermission_access numbers

What to be done in future commits?
- GUI + normal bar plot,
- Protection of DB,


