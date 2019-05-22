package com.wojtasik;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.plaf.nimbus.State;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.Date;

public class Datasource {
    public static final String DB_NAME = "electData.db";
    public static final String BLOCKED_ADRESS = "http://webtask.future-processing.com:8069/blocked";
    public static final String CANDIDATES_ADRESS = "http://webtask.future-processing.com:8069/candidates";

    public static final String DATE_FORMAT = "yyyy-MM-dd";


    public static final String TABLE_BLOCKED = "blockedPers";
    public static final String COLUMN_BLOCKED_ID = "_id";
    public static final String COLUMN_BLOCKED_PESEL = "pesel";


    public static final String TABLE_CANDIDATES = "candidates";
    public static final String COLUMN_CANDIDATES_ID = "_id";
    public static final String COLUMN_CANDIDATES_NAME = "name";
    public static final String COLUMN_CANDIDATES_PARTYID = "partyId";
    public static final String COLUMN_CANDIDATES_VOTES = "votes";

    public static final String TABLE_PARTYS = "partys";
    public static final String COLUMN_PARTYS_ID = "_id";
    public static final String COLUMN_PARTYS_NAME = "name";
    public static final String COLUMN_PARTYS_VOTES = "votes";


    public static final String TABLE_VOTERS = "partys";
    public static final String COLUMN_VOTERS_ID = "_id";
    public static final String COLUMN_VOTERS_NAME = "name";
    public static final String COLUMN_VOTERS_PESEL = "pesel";
    public static final String COLUMN_VOTERS_VOTED = "voted";

    public static final String TABLE_VALVOTES = "valVotes";
    public static final String COLUMN_VALVOTES_STATUS = "status";
    public static final String COLUMN_VALVOTES_NUMBER = "number";

    public static final String ADD_BLOCKED_PERSON = "INSERT INTO " + TABLE_BLOCKED + " (" + COLUMN_BLOCKED_PESEL + ") VALUES(\"";
    public static final String ADD_CANDIDATES = "INSERT INTO " + TABLE_CANDIDATES + " (" + COLUMN_CANDIDATES_NAME + ", " + COLUMN_CANDIDATES_PARTYID + ", " + COLUMN_CANDIDATES_VOTES + ") VALUES(\"";
    public static final String ADD_PARTY = "INSERT INTO " + TABLE_PARTYS + " (" + COLUMN_PARTYS_ID + ", " + COLUMN_PARTYS_NAME + ", " + COLUMN_PARTYS_VOTES + ") VALUES(";

    public static final String PRINT_CANDIDATES_AND_PARTY = "SELECT " + TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_ID + ", " + TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_NAME + ", " +
            TABLE_PARTYS + "." + COLUMN_PARTYS_NAME +", "+TABLE_CANDIDATES+"."+COLUMN_CANDIDATES_VOTES+ " FROM " + TABLE_CANDIDATES + " INNER JOIN " + TABLE_PARTYS + " ON " +
            TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_PARTYID + "=" + TABLE_PARTYS + "." + COLUMN_PARTYS_ID +
            " ORDER BY " + TABLE_CANDIDATES + "." + COLUMN_CANDIDATES_ID;
    public static final String CHECK_BLOCK_PESEL = "SELECT * FROM " + TABLE_BLOCKED + " WHERE " + COLUMN_BLOCKED_PESEL + "=\"";
    public static final String QUERY_TO_FIND_VOTER = "SELECT * FROM " + TABLE_VOTERS + " WHERE " + COLUMN_VOTERS_PESEL + "=\"";

    public static final String ADD_VOTER_TO_VOTE_LIST = "INSERT INTO " + TABLE_VOTERS + " (" + COLUMN_VOTERS_NAME +", "+COLUMN_VOTERS_PESEL+", "+COLUMN_VOTERS_VOTED+") VALUES (\"";

    public static final String QUERY_FOR_CANDIDATE = "SELECT * FROM "+TABLE_CANDIDATES+" WHERE "+COLUMN_CANDIDATES_ID+"=?";
    public static final String UPDATE_VOTES_FOR_CANDIDATE = "UPDATE "+TABLE_CANDIDATES+" SET "+COLUMN_CANDIDATES_VOTES+"=? WHERE "+COLUMN_CANDIDATES_ID+"=?";

    public static final String QUERY_FOR_PARTY = "SELECT * FROM "+TABLE_PARTYS+" WHERE "+COLUMN_PARTYS_ID+"=?";
    public static final String UPDATE_VOTES_FOR_PARTY = "UPDATE "+TABLE_PARTYS+" SET "+COLUMN_PARTYS_VOTES+"=? WHERE "+COLUMN_PARTYS_ID+"=?";

    public static final String QUERY_FOR_VALVOTES = "SELECT * FROM "+TABLE_VALVOTES+" WHERE "+COLUMN_VALVOTES_STATUS+"=?";
    public static final String UPDATE_NUMBER_FOR_VALVOTES = "UPDATE "+TABLE_VALVOTES+" SET "+COLUMN_VALVOTES_NUMBER+"=? WHERE "+COLUMN_VALVOTES_STATUS+"=?";

    public static final String QUERY_CANDIDATES_STATISTIC = "SELECT "+COLUMN_CANDIDATES_VOTES+", "+COLUMN_CANDIDATES_NAME+" FROM "+TABLE_CANDIDATES+" ORDER BY "+COLUMN_CANDIDATES_VOTES+" DESC";
    public static final String QUERY_PARTIES_STATISTIC = "SELECT "+COLUMN_PARTYS_VOTES+", "+COLUMN_PARTYS_NAME+" FROM "+TABLE_PARTYS+" ORDER BY "+COLUMN_PARTYS_VOTES+" DESC";

    public static final int HARD_HASH_VALUE = 7;


    private Connection conn;

    public boolean open() {
        try {
            //Checking current directory on disk
            Path currentRelativePath = Path.of("");
            String adress = ("jdbc:sqlite:" + currentRelativePath.toAbsolutePath().toString() + "/src/com/wojtasik/" + DB_NAME);
            conn = DriverManager.getConnection(adress);
            System.out.println("Connected");
            updateBlockedPeople();
            cleanTable(TABLE_VALVOTES);
            prepareStatisticsTable();
            cleanTable(TABLE_CANDIDATES);
            cleanTable(TABLE_PARTYS);
            setCandidatePartyRelation(getCandidatesFromXML());

            //Prepared statements


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

    public void printTables() {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet result = metaData.getTables(null, null, "%", null);
            while (result.next()) {
                System.out.println(result.getString(3));
            }
        } catch (SQLException e) {
            System.out.println("ERROR; PRINT TABLES : " + e.getMessage());
        }
    }

    public void printCandidates() {
        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(PRINT_CANDIDATES_AND_PARTY);
            while (result.next()) {
                System.out.println(result.getString(1) + "\t" + result.getString(2) + "\t\t" + result.getString(3));
            }
        } catch (SQLException e) {
            e.getStackTrace();
        }
    }

    public int numberOfCandidates() {
        int counter=0;
        try {
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(PRINT_CANDIDATES_AND_PARTY);
            while (result.next()) {
                counter+=1;
            }
        } catch (SQLException e) {
            e.getStackTrace();
        }
        return counter;
    }
    /**
     * This method is to check if user choosed only one valid option.
     * Advantage of String using is that even if user would choose multiple choices
     *  i.e. "1, 5" or "12 4" program will accept that input but it will not be valid
     */
    private boolean voteValidation(String choose){
        try {
            int chooseInt = Integer.parseInt(choose);
            if(chooseInt>=1 && chooseInt<=numberOfCandidates()){
                return true;
            }else{
                return false;
            }
        }catch (NumberFormatException e){
            return false;
        }
    }


    public void vote(String name, String pesel, String choose){
        if(voteValidation(choose)){
            addVoterToDB(name, pesel, 1);
            voteForCandidate(Integer.parseInt(choose));
            addStatistics("valid");
        }else{
            addVoterToDB(name, pesel, 1);
            addStatistics("nonvalid");
        }

    }

    private void voteForCandidate(int candNumber){
        try {
            PreparedStatement findStatement = conn.prepareStatement(QUERY_FOR_CANDIDATE);
            findStatement.setInt(1, candNumber);
            ResultSet findResult = findStatement.executeQuery();
            int actualVotes = findResult.getInt(4);
            PreparedStatement updateStatement = conn.prepareStatement(UPDATE_VOTES_FOR_CANDIDATE);
            updateStatement.setInt(1, actualVotes+1);
            updateStatement.setInt(2, findResult.getInt(1));
            updateStatement.execute();
            voteForParty(findResult.getInt(3));
            updateStatement.close();
            findStatement.close();
        } catch (SQLException e) {
            e.getStackTrace();
        }
    }

    private void voteForParty(int partyNumber){
        try{
            PreparedStatement findPartyStatement = conn.prepareStatement(QUERY_FOR_PARTY);
            findPartyStatement.setInt(1, partyNumber);
            ResultSet findPartyResult = findPartyStatement.executeQuery();
            int actualVotes = findPartyResult.getInt(3);
            PreparedStatement updateStatement = conn.prepareStatement(UPDATE_VOTES_FOR_PARTY);
            updateStatement.setInt(1, actualVotes+1);
            updateStatement.setInt(2, findPartyResult.getInt(1));
            updateStatement.execute();

            updateStatement.close();
            findPartyStatement.close();
        }catch (SQLException e){
            e.getMessage();
        }
    }

    public void printVoteResults(){

        try{
            Statement candidatesStatement = conn.createStatement();
            ResultSet candidatesResult = candidatesStatement.executeQuery(QUERY_CANDIDATES_STATISTIC);
            System.out.println("==== CANDIDATES VOTING RESULTS ====");
            while(candidatesResult.next()){
                System.out.println(candidatesResult.getInt(1)+" : \t"+candidatesResult.getString(2));
            }
            Statement partyStatement = conn.createStatement();
            ResultSet partyResult = partyStatement.executeQuery(QUERY_PARTIES_STATISTIC);
            System.out.println("==== PARTIES VOTING RESULTS ====");
            while(partyResult.next()){
                System.out.println(partyResult.getInt(1)+" : \t"+partyResult.getString(2));
            }
            PreparedStatement valVoteStatement = conn.prepareStatement(QUERY_FOR_VALVOTES);
            valVoteStatement.setString(1, "nonvalid");
            ResultSet valVoteResult = valVoteStatement.executeQuery();
            System.out.println("Non valid votes : "+valVoteResult.getInt(2));

        }catch (SQLException e){
            e.getMessage();
        }
    }

    private void prepareStatisticsTable(){
        try{
            Statement statement = conn.createStatement();
            statement.execute("INSERT INTO "+TABLE_VALVOTES+" ("+COLUMN_VALVOTES_STATUS+", "+COLUMN_VALVOTES_NUMBER+") VALUES (\"valid\", 0)");
            statement.execute("INSERT INTO "+TABLE_VALVOTES+" ("+COLUMN_VALVOTES_STATUS+", "+COLUMN_VALVOTES_NUMBER+") VALUES (\"nonvalid\", 0)");
            statement.execute("INSERT INTO "+TABLE_VALVOTES+" ("+COLUMN_VALVOTES_STATUS+", "+COLUMN_VALVOTES_NUMBER+") VALUES (\"nopermission\", 0)");
        }catch (SQLException e){
            e.getMessage();
        }
    }

    public void addStatistics(String status){
        try{
            PreparedStatement findStatement = conn.prepareStatement(QUERY_FOR_VALVOTES);
            findStatement.setString(1, status);
            ResultSet result = findStatement.executeQuery();
            int actualVotes = result.getInt(2);
            System.out.println(result.getString(1)+"  = > "+result.getInt(2));

            PreparedStatement updateStatement = conn.prepareStatement(UPDATE_NUMBER_FOR_VALVOTES);
            updateStatement.setInt(1, actualVotes+1);
            updateStatement.setString(2, status);
            updateStatement.execute();

            updateStatement.close();
            findStatement.close();
        }catch (SQLException e){
            e.getMessage();
        }
    }

    private void addVoterToDB(String name, String pesel, int voted){
        try{
            Statement statement = conn.createStatement();
            String insert = ADD_VOTER_TO_VOTE_LIST+name+"\", \""+pesel+"\", "+voted+")";
            statement.execute(insert);
        }catch (SQLException e){
            e.getMessage();
        }
    }

    private boolean nameValidate(String name) {
        if (!name.matches("[0-9]+")) {
            return true;
        } else {
            System.out.println("Wrong name input.");
            return false;
        }
    }

    public boolean votingAccess(String name, String pesel) {
        if (!nameValidate(name) || !peselValidate(pesel)) {
            return false;
        }
        if (isBlocked(pesel) || !isAdult(getDateFromPesel(pesel))) {
            addStatistics("nopermission");
            return false;
        }
        if(hasAlreadyVoted(pesel)){
            return false;
        }
        return true;
    }

    private boolean peselValidate(String pesel) {
        if (pesel.matches("[0-9]+") && pesel.length() == 11) {
            return true;
        } else {
            System.out.println("Wrong pesel input.");
            return false;
        }
    }

    public LocalDate getDateFromPesel(String pesel) {
        String dateInfo = pesel.substring(0, 6);
        String strDate;
        //Checking if someone was born after 2000
        if (dateInfo.charAt(2) == '2' || dateInfo.charAt(2) == '3') {
            //Born after 2000
            strDate = "20" + dateInfo.substring(0, 2) + "-" + String.format("%02d", (Integer.parseInt(dateInfo.substring(2, 4))) - 20) + "-" + dateInfo.substring(4, 6);
        } else {
            strDate = "19" + dateInfo.substring(0, 2) + "-" + dateInfo.substring(2, 4) + "-" + dateInfo.substring(4, 6);
        }
//        try {
//            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
//            Date date1 = formatter.parse(strDate);
//            return date1;
            LocalDate date = LocalDate.parse(strDate);
//        }
//             catch (ParseException e) {
//            System.out.println("Problem occuring + " + e.getMessage());
//            return null;
        return date;
//        }
    }

    public boolean isAdult(LocalDate userDate) {
        System.out.println("Input " + userDate.toString());
        userDate = userDate.plusYears(18);
        LocalDate d = LocalDate.now();
        System.out.println("counted Date " + d.toString());
        if (userDate.isBefore(d.plusDays(1))){
            return true;
        }else{
            System.out.println("You are under 18");
            return false;
        }
    }

    public boolean isBlocked(String peselToBeChecked) {
        try {

            Statement statement = conn.createStatement();
            String query = CHECK_BLOCK_PESEL + peselToBeChecked + "\"";
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                return true;
            } else {
                System.out.println("You have no voting rights.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("here " + e.getMessage());
            return false;
        }
    }

    private boolean hasAlreadyVoted(String pesel){
        try {
            Statement statement = conn.createStatement();
            String query = QUERY_TO_FIND_VOTER + pesel + "\"";
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                if(resultSet.getString(4).equals("1")) {
                    System.out.println("You have already voted.");
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            System.out.println("here " + e.getMessage());
            return false;
        }
    }

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

    public InputStream connectionXML(String table) {
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

    private void updateBlockedPeople() {
        cleanTable(TABLE_BLOCKED);
        Document inputDoc = inputFromUrl(connectionXML(BLOCKED_ADRESS));
        try {
            NodeList blocked = inputDoc.getElementsByTagName("pesel");

            for (int i = 0; i < blocked.getLength(); i++) {
                Node n = blocked.item(i);
                try {
                    Statement statement = conn.createStatement();
                    String insertion = (ADD_BLOCKED_PERSON + n.getTextContent() + "\")");
//                    System.out.println(insertion);
                    statement.execute(insertion);
                } catch (SQLException e) {
                    e.getMessage();
                }
                System.out.println(i + " " + n.getTextContent());
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }


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
            for (String text : candidateList.keySet()) {
                System.out.println(text + " === " + candidateList.get(text));
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return candidateList;
    }

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
        System.out.println();
        for (Candidate cand : candidateSet) {
            System.out.println(cand.getName() + " : " + cand.getParty().getId() + " : " + cand.getParty().getName());
        }
        updatePartys(partiesSet);
        updateCandidates(candidateSet);
    }

    private void updateCandidates(Set<Candidate> candidateSet) {

        try {
            Statement statement = conn.createStatement();
            for (Candidate cand : candidateSet) {
                String candidateAdding = ADD_CANDIDATES + cand.getName() + "\", " + cand.getParty().getId() + ", " + cand.getVotes() + ")";
                System.out.println(candidateAdding);
                statement.execute(candidateAdding);
//                statement.execute(partyAdding);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void updatePartys(Set<Party> setPartys) {
        try {
            Statement statement = conn.createStatement();
            for (Party part : setPartys) {
                String partyAdding = ADD_PARTY + part.getId() + ", \"" + part.getName() + "\", " + part.getVotes() + ")";
                System.out.println(partyAdding);
                statement.execute(partyAdding);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    private int searchForParty(String party, Set<Party> partieSet) {
        for (Party part : partieSet) {
            if (part.getName().equals(party)) {
                return part.getId();
            }
        }
        return 0;
    }


    private void cleanTable(String tableName) {
        try {
            Statement statement = conn.createStatement();
            statement.execute("DELETE FROM " + tableName);
        } catch (SQLException e) {
            e.getMessage();
        }
    }


}
