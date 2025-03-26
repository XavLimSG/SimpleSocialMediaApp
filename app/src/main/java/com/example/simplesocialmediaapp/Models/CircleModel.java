package com.example.simplesocialmediaapp.Models;

import java.util.ArrayList;

public class CircleModel {
    private String circleId;
    private String circleName;
    private ArrayList<String> members;

    public CircleModel() {} // needed for Firebase

    public CircleModel(String circleId, String circleName, ArrayList<String> members) {
        this.circleId = circleId;
        this.circleName = circleName;
        this.members = members;
    }

    public String getCircleId() { return circleId; }
    public void setCircleId(String circleId) { this.circleId = circleId; }

    public String getCircleName() { return circleName; }
    public void setCircleName(String circleName) { this.circleName = circleName; }

    public ArrayList<String> getMembers() { return members; }
    public void setMembers(ArrayList<String> members) { this.members = members; }
}

