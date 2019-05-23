package com.wojtasik;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class Datasource {
    private static final String DB_NAME = "electData.db";
    private static final String BLOCKED_ADRESS = "http://webtask.future-processing.com:8069/blocked";
    private static final String CANDIDATES_ADRESS = "http://webtask.future-processing.com:8069/candidates";

    private static final String TABLE_BLOCKED = "blockedPers";
    private static final String COLUMN_BLOCKED_ID = "_id";
    private static final String COLUMN_BLOCKED_PESEL = "pesel";


    private static final String TABLE_CANDIDATES = "candidates";
    private static final String COLUMN_CANDIDATES_ID = "_id";
    private static final String COLUMN_CANDIDATES_NAME = "name";
    private static final String COLUMN_CANDIDATES_PARTYID = "partyId";
    private static final String COLUMN_CANDIDATES_VOTES = "votes";

    private static final String TABLE_PARTYS = "partys";
    private static final String COLUMN_PARTYS_ID = "_id";
    private static final String COLUMN_PARTYS_NAME = "name";
    private static final String COLUMN_PARTYS_VOTES = "votes";


    private static final String TABLE_VOTERS = "voters";
    private static final String COLUMN_VOTERS_ID = "_id";
    private static final String COLUMN_VOTERS_NAME = "name";
    private static final String COLUMN_VOTERS_PESEL = "pesel";
    private static final String COLUMN_VOTERS_VOTED = "voted";

    private static final String TABLE_VALVOTES = "valVotes";
    private static final String COLUMN_VALVOTES_STATUS = "status";
    private static final String COLUMN_VALVOTES_NUMBER = "number";

    private static final String ADD_BLOCKED_PERSON = "INSERT INTO " + TABLE_BLOCKED + " (" + COLUMN_BLOCKED_PESEL + ") VALUES(?)";
    private static final String ADD_CANDIDATES = "INSERT INTO " + TABLE_CANDIDATES + " (" + COLUMN_CANDIDATES_NAME + ", " + COLUMN_CANDIDATES_PARTYID + ", " + COLUMN_CANDIDATES_VOTES + ") VALUES(?,?,?)";
    private static final String ADD_PARTY = "INSERT INTO " + TABLE_PARTYS + " (" + COLUMN_PARTYS_ID + ", " + COLUMN_PARTYS_NAME + ", " + COLUMN_PARTYS_VOTES + ") VALUES(?,?,?)";

    private static final String PRINT_CANDIDATES_AND_PARTY = "SELECT " + TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_ID + ", " + TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_NAME + ", " +
            TABLE_PARTYS + "." + COLUMN_PARTYS_NAME + ", " + TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_VOTES + " FROM " + TABLE_CANDIDATES + " INNER JOIN " + TABLE_PARTYS + " ON " +
            TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_PARTYID + "=" + TABLE_PARTYS + "." + COLUMN_PARTYS_ID +
            " ORDER BY " + TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_ID;
    private static final String CHECK_BLOCK_PESEL = "SELECT * FROM " + TABLE_BLOCKED + " WHERE " + COLUMN_BLOCKED_PESEL + "=?";
    private static final String QUERY_TO_FIND_VOTER = "SELECT * FROM " + TABLE_VOTERS + " WHERE " + COLUMN_VOTERS_PESEL + "=?";

    private static final String ADD_VOTER_TO_VOTE_LIST = "INSERT INTO " + TABLE_VOTERS + " (" + COLUMN_VOTERS_NAME + ", " + COLUMN_VOTERS_PESEL + ", " + COLUMN_VOTERS_VOTED + ") VALUES(?,?,?)";

    private static final String QUERY_FOR_CANDIDATE = "SELECT * FROM " + TABLE_CANDIDATES + " WHERE " + COLUMN_CANDIDATES_ID + "=?";
    private static final String UPDATE_VOTES_FOR_CANDIDATE = "UPDATE " + TABLE_CANDIDATES + " SET " + COLUMN_CANDIDATES_VOTES + "=? WHERE " + COLUMN_CANDIDATES_ID + "=?";

    private static final String QUERY_FOR_PARTY = "SELECT * FROM " + TABLE_PARTYS + " WHERE " + COLUMN_PARTYS_ID + "=?";
    private static final String UPDATE_VOTES_FOR_PARTY = "UPDATE " + TABLE_PARTYS + " SET " + COLUMN_PARTYS_VOTES + "=? WHERE " + COLUMN_PARTYS_ID + "=?";

    private static final String QUERY_FOR_VALVOTES = "SELECT * FROM " + TABLE_VALVOTES + " WHERE " + COLUMN_VALVOTES_STATUS + "=?";
    private static final String UPDATE_NUMBER_FOR_VALVOTES = "UPDATE " + TABLE_VALVOTES + " SET " + COLUMN_VALVOTES_NUMBER + "=? WHERE " + COLUMN_VALVOTES_STATUS + "=?";

    private static final String QUERY_CANDIDATES_STATISTIC = "SELECT " + COLUMN_CANDIDATES_VOTES + ", " + COLUMN_CANDIDATES_NAME + " FROM " + TABLE_CANDIDATES + " ORDER BY " + COLUMN_CANDIDATES_VOTES + " DESC";
    private static final String QUERY_PARTIES_STATISTIC = "SELECT " + COLUMN_PARTYS_VOTES + ", " + COLUMN_PARTYS_NAME + " FROM " + TABLE_PARTYS + " ORDER BY " + COLUMN_PARTYS_VOTES + " DESC";

    private static final int HARD_HASH_VALUE = 3;


    private Connection conn;

    public boolean open() {
        try {
            Path currentRelativePath = Path.of("");
            String adress = ("jdbc:sqlite:" + currentRelativePath.toAbsolutePath().toString() + "/" + DB_NAME);
            conn = DriverManager.getConnection(adress);
            System.out.println("Connected to database");
            updateBlockedPeople();
            cleanTable(TABLE_VALVOTES);
            prepareStatisticsTable();
            cleanTable(TABLE_CANDIDATES);
            cleanTable(TABLE_PARTYS);
            setCandidatePartyRelation(getCandidatesFromXML());
            return true;
        } catch (SQLException e) {
            System.out.println("Couldn't connect to database: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Couldn't close connection: " + e.getMessage());
        }
    }

    /**
     * Code or decode String value. Coding/decoding depends on sgn of given key)
     */
    private String simpleHash(String message, int key) {
        String s = "";
        int len = message.length();
        for (int x = 0; x < len; x++) {
            s += (char) (message.charAt(x) + key * x);
        }
        return s;
    }
    /**
     * Code String value with a static key
     */
    private String codePesel(String text) {
        return simpleHash(text, HARD_HASH_VALUE);
    }

    /**
     * This method is to check if user choosed only one valid option.
     * Advantage of String using is that even if user would choose multiple choices
     * i.e. "1, 5" or "12 4" program will accept that input but it will not be valid
     */
    private boolean voteValidation(String choose) {
        try {
            int chooseInt = Integer.parseInt(choose);
            if (chooseInt >= 1 && chooseInt <= numberOfCandidates()) {
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Return number of Candidates
     */
    private int numberOfCandidates() {
        int counter = 0;
        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(PRINT_CANDIDATES_AND_PARTY);
            while (result.next()) {
                counter += 1;
            }
        } catch (SQLException e) {
            e.getStackTrace();
        }
        return counter;
    }

    /**
     * Return boolean value if user is allowed to vote.
     * Tests and access depends only on pesel number
     */
    public boolean votingAccess(String name, String pesel) {
        if (!nameValidate(name) || !peselValidate(pesel)) {
            return false;
        }
        if (isBlocked(pesel) || !isAdult(getDateFromPesel(pesel))) {
            addStatistics("nopermission");
            return false;
        }
        if (hasAlreadyVoted(pesel)) {
            return false;
        }
        return true;
    }
    /**
     * Return true if user is at least 18 while login.
     * Return false otherwise
     */
    private boolean isAdult(LocalDate userDate) {
        userDate = userDate.plusYears(18);
        LocalDate d = LocalDate.now();
        if (userDate.isBefore(d.plusDays(1))) {
            return true;
        } else {
            System.out.println("You are under 18");
            return false;
        }
    }
    /**
     * Return true if user has voting rights,
     * pesel number is hashing and comparing with hashed values in database
     */
    private boolean isBlocked(String peselToBeChecked) {
        try {
            updateBlockedPeople();
            PreparedStatement statement = conn.prepareStatement(CHECK_BLOCK_PESEL);
            statement.setString(1, codePesel(peselToBeChecked));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                System.out.println("You have no voting rights.");
                statement.close();
                return true;
            } else {
                statement.close();
                return false;
            }

        } catch (SQLException e) {
            e.getMessage();
            return false;
        }
    }
    /**
     * Return true if user has already voted,
     * return false if this is his first voting
     */
    private boolean hasAlreadyVoted(String pesel) {
        try {
            PreparedStatement statement = conn.prepareStatement(QUERY_TO_FIND_VOTER);
            statement.setString(1, codePesel(pesel));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                System.out.println("You have already voted.");
                statement.close();
                return true;
            }
            statement.close();
            return false;
        } catch (SQLException e) {
            System.out.println("here " + e.getMessage());
            return false;
        }
    }
    /**
     * This method calls rest private methods to manage voting process
     * and statistics update
     */
    public void vote(String name, String pesel, String choose) {
        if (voteValidation(choose)) {
            addVoterToDB(name, pesel, 1);
            voteForCandidate(Integer.parseInt(choose));
            addStatistics("valid");
        } else {
            addVoterToDB(name, pesel, 1);
            addStatistics("nonvalid");
        }
    }
    /**
     * Print actual number of candidate, name of candidate and party
     */
    public void printCandidates() {
        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(PRINT_CANDIDATES_AND_PARTY);
            while (result.next()) {
                System.out.println(result.getString(1) + "\t" + result.getString(2) + "\t\t" + result.getString(3));
            }
            statement.close();
        } catch (SQLException e) {
            e.getStackTrace();
        }
    }

    /**
     * Search for candidate in database based on his number on voting list
     * and increase votes field.
     */
    private void voteForCandidate(int candNumber) {
        try {
            PreparedStatement findStatement = conn.prepareStatement(QUERY_FOR_CANDIDATE);
            findStatement.setInt(1, candNumber);
            ResultSet findResult = findStatement.executeQuery();
            int actualVotes = findResult.getInt(4);
            PreparedStatement updateStatement = conn.prepareStatement(UPDATE_VOTES_FOR_CANDIDATE);
            updateStatement.setInt(1, actualVotes + 1);
            updateStatement.setInt(2, findResult.getInt(1));
            updateStatement.execute();
            voteForParty(findResult.getInt(3));
            updateStatement.close();
            findStatement.close();
        } catch (SQLException e) {
            e.getStackTrace();
        }
    }
    /**
     * Search for party in database based on choose candidate
     * and increase votes field.
     */
    private void voteForParty(int partyNumber) {
        try {
            PreparedStatement findPartyStatement = conn.prepareStatement(QUERY_FOR_PARTY);
            findPartyStatement.setInt(1, partyNumber);
            ResultSet findPartyResult = findPartyStatement.executeQuery();
            int actualVotes = findPartyResult.getInt(3);
            PreparedStatement updateStatement = conn.prepareStatement(UPDATE_VOTES_FOR_PARTY);
            updateStatement.setInt(1, actualVotes + 1);
            updateStatement.setInt(2, findPartyResult.getInt(1));
            updateStatement.execute();

            updateStatement.close();
            findPartyStatement.close();
        } catch (SQLException e) {
            e.getMessage();
        }
    }
    /**
     * Create export.csv file on local disk with voting statistics
     */
    public void generateCSV() {
        try {
            ResultSet noPermitVoteResult = getVotesAmount(QUERY_FOR_VALVOTES, "nopermission");
            int noPermissionVotes = noPermitVoteResult.getInt(2);
            ResultSet valVoteResult = getVotesAmount(QUERY_FOR_VALVOTES, "valid");
            int validVotes = valVoteResult.getInt(2);
            ResultSet nonVoteResult = getVotesAmount(QUERY_FOR_VALVOTES, "nonvalid");
            int nonValidVotes = nonVoteResult.getInt(2);

            Statement canStatement = conn.createStatement();
            ResultSet candidatesResult = canStatement.executeQuery(QUERY_CANDIDATES_STATISTIC);

            Statement partyStatement = conn.createStatement();
            ResultSet partyResult = partyStatement.executeQuery(QUERY_PARTIES_STATISTIC);

            FileWriter csvFile = new FileWriter("export.csv");
            csvFile.append("CandidateName");
            csvFile.append(",");
            csvFile.append("VotesNumber");
            csvFile.append(",");
            csvFile.append("Percent");
            csvFile.append("\n");
            while (candidatesResult.next()) {
                double perc = givePercents(validVotes, candidatesResult.getInt(1));
                csvFile.append(candidatesResult.getString(2));
                csvFile.append(",");
                csvFile.append(candidatesResult.getString(1));
                csvFile.append(",");
                csvFile.append(Double.toString(perc));
                csvFile.append("\n");
            }
            csvFile.append("\n");
            while (partyResult.next()) {
                double perc = givePercents(validVotes, partyResult.getInt(1));
                csvFile.append(partyResult.getString(2));
                csvFile.append(",");
                csvFile.append(partyResult.getString(1));
                csvFile.append(",");
                csvFile.append(Double.toString(perc));
                csvFile.append("\n");
            }
            csvFile.append("\n");
            csvFile.append("ValidVotes");
            csvFile.append(",");
            csvFile.append("NonValidVotes");
            csvFile.append(",");
            csvFile.append("BlockedAccesses");
            csvFile.append("\n");
            csvFile.append(Integer.toString(validVotes));
            csvFile.append(",");
            csvFile.append(Integer.toString(nonValidVotes));
            csvFile.append(",");
            csvFile.append(Integer.toString(noPermissionVotes));
            csvFile.append("\n");

            csvFile.flush();
            csvFile.close();

        } catch (Exception e){
            e.getMessage();
        }
    }
    /**
     * Print voting statistics
     */
    public void printVoteResults(String barGraph, String toReport) {

        try {
            ResultSet noPermitVoteResult = getVotesAmount(QUERY_FOR_VALVOTES, "nopermission");
            int noPermissionVotes = noPermitVoteResult.getInt(2);
            ResultSet valVoteResult = getVotesAmount(QUERY_FOR_VALVOTES, "valid");
            int validVotes = valVoteResult.getInt(2);
            ResultSet nonVoteResult = getVotesAmount(QUERY_FOR_VALVOTES, "nonvalid");
            int nonValidVotes = nonVoteResult.getInt(2);

            System.out.println("==== CANDIDATES VOTING RESULTS ====");
            Statement canStatement = conn.createStatement();
            ResultSet candidatesResult = canStatement.executeQuery(QUERY_CANDIDATES_STATISTIC);
            while (candidatesResult.next()) {
                double perc = givePercents(validVotes, candidatesResult.getInt(1));
                if (barGraph.equals("Y")) {
                    System.out.println(candidatesResult.getInt(1) + " :  " + perc + "%\t" + barDraw(perc) + "\t\t:  " + candidatesResult.getString(2));
                } else {
                    System.out.println(candidatesResult.getInt(1) + " :  " + perc + "%\t:  " + candidatesResult.getString(2));
                }
            }
            Statement partyStatement = conn.createStatement();
            ResultSet partyResult = partyStatement.executeQuery(QUERY_PARTIES_STATISTIC);
            System.out.println("==== PARTIES VOTING RESULTS ====");
            while (partyResult.next()) {

                double perc = givePercents(validVotes, partyResult.getInt(1));
                if (barGraph.equals("Y")) {
                    System.out.println(partyResult.getInt(1) + " : " + perc + "%\t" + barDraw(perc) + "\t\t:  " + partyResult.getString(2));
                } else {
                    System.out.println(partyResult.getInt(1) + " : " + perc + "%\t:  " + partyResult.getString(2));
                }
            }
            if (toReport.equals("Y")) {
                System.out.println("\n" + validVotes + "\t : Number of valid votes");
                System.out.println(nonValidVotes + "\t : Number of nonvalid votes");
                System.out.println(noPermissionVotes + "\t : Trying to vote without permission\n");
            }else {
                System.out.println(nonValidVotes + "\t : Number of nonvalid votes");
            }
            canStatement.close();
            partyStatement.close();
        } catch (SQLException e) {
            e.getMessage();
        }
    }
    /**
     * Return rounded percent value based on two inputs
     */
    private double givePercents(int whole, int fraction) {
        double dWhole = whole;
        double dFraction = fraction;
        double percent = (dFraction / dWhole) * 100;
        double decimal = Math.pow(10, 1);
        return Math.round(percent * decimal) / decimal;
    }
    /**
     * GUI for people without live taste
     */
    private String barDraw(double perc) {
        String bar = "";
        for (int i = 0; i <= (perc / 10); i++) {
            bar += "|";
        }
        return bar;
    }
    /**
     * Return ResultSet with valid,nonvalid votes and blocked logs
     */
    private ResultSet getVotesAmount(String query, String voteType) {
        try {
            PreparedStatement valVoteStatement = conn.prepareStatement(query);
            valVoteStatement.setString(1, voteType);
            ResultSet valVoteResult = valVoteStatement.executeQuery();
            return valVoteResult;
        } catch (
                SQLException e) {
            e.getMessage();
            return null;
        }
    }
    /**
     * create empty fields for valid,nonvalid votes and blocked logs
     * in valVotes table
     */
    private void prepareStatisticsTable() {
        try {
            Statement statement = conn.createStatement();
            statement.execute("INSERT INTO " + TABLE_VALVOTES + " (" + COLUMN_VALVOTES_STATUS + ", " + COLUMN_VALVOTES_NUMBER + ") VALUES (\"valid\", 0)");
            statement.execute("INSERT INTO " + TABLE_VALVOTES + " (" + COLUMN_VALVOTES_STATUS + ", " + COLUMN_VALVOTES_NUMBER + ") VALUES (\"nonvalid\", 0)");
            statement.execute("INSERT INTO " + TABLE_VALVOTES + " (" + COLUMN_VALVOTES_STATUS + ", " + COLUMN_VALVOTES_NUMBER + ") VALUES (\"nopermission\", 0)");
            statement.close();
        } catch (SQLException e) {
            e.getMessage();
        }
    }
    /**
     * update fields for valid,nonvalid votes and blocked logs
     * in valVotes table
     */
    private void addStatistics(String status) {
        try {
            PreparedStatement findStatement = conn.prepareStatement(QUERY_FOR_VALVOTES);
            findStatement.setString(1, status);
            ResultSet result = findStatement.executeQuery();
            int actualVotes = result.getInt(2);
//            System.out.println(result.getString(1)+"  = > "+result.getInt(2));

            PreparedStatement updateStatement = conn.prepareStatement(UPDATE_NUMBER_FOR_VALVOTES);
            updateStatement.setInt(1, actualVotes + 1);
            updateStatement.setString(2, status);
            updateStatement.execute();

            updateStatement.close();
            findStatement.close();
        } catch (SQLException e) {
            e.getMessage();
        }
    }
    /**
     * add successfully logged user to the database
     */
    private void addVoterToDB(String name, String pesel, int voted) {
        try {
            PreparedStatement statement = conn.prepareStatement(ADD_VOTER_TO_VOTE_LIST);
            statement.setString(1, name);
            statement.setString(2, codePesel(pesel));
            statement.setInt(3, voted);
            statement.execute();
            statement.close();
        } catch (SQLException e) {
            e.getMessage();
        }
    }

    /**
     * return true if name doesn't contain any numbers
     * return false otherwise
     */
    private boolean nameValidate(String name) {
        if (!name.matches("[0-9]+")) {
            return true;
        } else {
            System.out.println("Wrong name input.");
            return false;
        }
    }

    /**
     * Return true if pesel doesn't contain any letters
     * and is 11 digits long.
     * */
    private boolean peselValidate(String pesel) {
        if (pesel.matches("[0-9]+") && pesel.length() == 11) {
            return true;
        } else {
            System.out.println("Wrong pesel input.");
            return false;
        }
    }
    /**
     * Return date based on pesel number
     */
    private LocalDate getDateFromPesel(String pesel) {
        String dateInfo = pesel.substring(0, 6);
        String strDate;
        //Checking if someone was born after 2000
        if (dateInfo.charAt(2) == '2' || dateInfo.charAt(2) == '3') {
            //Born after 2000
            strDate = "20" + dateInfo.substring(0, 2) + "-" + String.format("%02d", (Integer.parseInt(dateInfo.substring(2, 4))) - 20) + "-" + dateInfo.substring(4, 6);
        } else {
            strDate = "19" + dateInfo.substring(0, 2) + "-" + dateInfo.substring(2, 4) + "-" + dateInfo.substring(4, 6);
        }
        LocalDate date = LocalDate.parse(strDate);

        return date;
    }

    /**
     * update people wihtout no voting rights on DB
     * */
    private void updateBlockedPeople() {
        cleanTable(TABLE_BLOCKED);
        Document inputDoc = inputFromUrl(connectionXML(BLOCKED_ADRESS));
        try {
            NodeList blocked = inputDoc.getElementsByTagName("pesel");

            for (int i = 0; i < blocked.getLength(); i++) {
                Node n = blocked.item(i);
                try {
                    PreparedStatement statement = conn.prepareStatement(ADD_BLOCKED_PERSON);
                    statement.setString(1, codePesel(n.getTextContent()));
                    statement.execute();
                    statement.close();
                } catch (SQLException e) {
                    e.getMessage();
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * Return document via internet stream
     * */
    private Document inputFromUrl(InputStream stream) {
        Document ret = null;
        DocumentBuilderFactory domFactory;
        DocumentBuilder builder;
        try {
            domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setValidating(false);
            domFactory.setNamespaceAware(false);
            builder = domFactory.newDocumentBuilder();
            ret = builder.parse(stream);
        } catch (Exception e) {
            System.out.println("Unable to read XML : " + e.getMessage());
        }
        return ret;
    }
    /**
     * return InputStream for a URL
     * also set connection request to get needed document format (XML)
     * */
    private InputStream connectionXML(String table) {
        try {
            URL url = new URL(table);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/xml");
            return connection.getInputStream();
        } catch (IOException e) {
            e.getMessage();
            return null;
        }
    }
    /**
     * Return Map with Candidate name - Party name pairs
     * */
    private Map<String, String> getCandidatesFromXML() {
        Document inputDoc = inputFromUrl(connectionXML(CANDIDATES_ADRESS));
        Map<String, String> candidateList = new HashMap<>();
        try {
            NodeList candidates = inputDoc.getElementsByTagName("candidate");
            for (int i = 0; i < candidates.getLength(); i++) {
                Node n = candidates.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element candidate = (Element) n;
                    ArrayList<String> datas = new ArrayList<>();
                    NodeList details = candidate.getChildNodes();
                    for (int j = 0; j < details.getLength(); j++) {
                        Node d = details.item(j);
                        if (d.getNodeType() == Node.ELEMENT_NODE) {
                            Element specified = (Element) d;
                            datas.add(specified.getTextContent());
                        }
                    }
                    candidateList.put(datas.get(0), datas.get(1));
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return candidateList;
    }

    /**
     * Based on candidate-party input creates Candidate and Party unique objects
     * and add them to HashSets. HashSet structure was choose on purpose
     * */
    private void setCandidatePartyRelation(Map<String, String> candidateMap) {
        Set<Party> partiesSet = new HashSet<>();
        Set<Candidate> candidateSet = new HashSet<>();
        int partyCounter = 1;
        for (String cand : candidateMap.keySet()) {
            Party tempParty = new Party(candidateMap.get(cand), partyCounter);
            if (partiesSet.add(tempParty)) {
                partyCounter++;
            } else {
                tempParty = new Party(candidateMap.get(cand), searchForParty(candidateMap.get(cand), partiesSet));
            }
            Candidate tempCand = new Candidate(cand, tempParty);
            candidateSet.add(tempCand);
        }
        updateParties(partiesSet);
        updateCandidates(candidateSet);
    }
    /**
     * Add candidates to database
     * */
    private void updateCandidates(Set<Candidate> candidateSet) {
        try {
            PreparedStatement statement = conn.prepareStatement(ADD_CANDIDATES);
            for (Candidate cand : candidateSet) {
                statement.setString(1,cand.getName());
                statement.setInt(2,cand.getParty().getId());
                statement.setInt(3,cand.getVotes());
                statement.execute();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    /**
     * Add parties to database
     * */
    private void updateParties(Set<Party> setPartys) {
        try {
            PreparedStatement statement = conn.prepareStatement(ADD_PARTY);
            for (Party part : setPartys) {
                statement.setInt(1, part.getId());
                statement.setString(2, part.getName());
                statement.setInt(3,part.getVotes() );
                statement.execute();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    /**
     * Return id party in database based on name
     * */
    private int searchForParty(String party, Set<Party> partieSet) {
        for (Party part : partieSet) {
            if (part.getName().equals(party)) {
                return part.getId();
            }
        }
        return 0;
    }
    /**
     * Clean needed tables. Calls while opening connection.
     * */
    private void cleanTable(String tableName) {
        try {
            Statement statement = conn.createStatement();
            statement.execute("DELETE FROM " + tableName);
            statement.close();
        } catch (SQLException e) {
            e.getMessage();
        }
    }


}
