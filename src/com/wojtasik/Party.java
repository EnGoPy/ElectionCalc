package com.wojtasik;

import java.util.Objects;

public class Party {
    private String name;
    private int id;
    private int votes=0;

    public Party(String name, int id) {
        this.name = name;
        this.id=id;
    }

    public String getName() {
        return name;
    }
    public int getId(){
        return id;
    }
    public void voteUp(){
        this.votes++;
    }
    public int getVotes(){
        return votes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Party party = (Party) o;
        return Objects.equals(name, party.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode()+31;
    }
}
