package com.wojtasik;

import java.util.Objects;

public class Candidate {
    private final String name;
    private final Party party;
    private int votes=0;


    public Candidate(String name, Party party) {
        this.name = name;
        this.party = party;
    }

    public void voteUp(){
        votes+=1;
    }

    public String getName() {
        return name;
    }

    public int getVotes() {
        return votes;
    }
    public Party getParty(){
        return this.party;
    }

    @Override
    public String toString() {
        return "Candidate: "+this.name+"\tVotes; "+this.votes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return Objects.equals(name, candidate.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode()+31;
    }
}
