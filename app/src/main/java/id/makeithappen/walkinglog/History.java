package id.makeithappen.walkinglog;

public class History {

    int id;
    String date;
    int step;
    String duration;
	double distance;
    int calorie;
    int frequency;

    // Empty constructor
	public History(){
		
	}

    // constructor
	public History(int id, String date, String duration, int step, double distance, int calorie, int frequency){
        this.id = id;
        this.date = date;
        this.step = step;
        this.duration = duration;
        this.distance = distance;
        this.calorie = calorie;
        this.frequency = frequency;
    }

    // constructor
    public History(int step, String duration, double distance, int calorie, int frequency){
        this.step = step;
        this.duration = duration;
        this.distance = distance;
        this.calorie = calorie;
        this.frequency = frequency;
    }

    // getting ID
    public int getID(){
        return this.id;
    }

    // setting id
    public void setID(int id){
        this.id = id;
    }

    // getting date
    public String getDate(){
        return this.date;
    }

    // setting date
    public void setDate(String date){ this.date = date; }

	// getting step
    public int getStep(){
        return this.step;
    }

    // setting step
    public void setStep(int step){
        this.step = step;
    }

    // getting duration
    public String getDuration(){
        return this.duration;
    }

    // setting duration
    public void setDuration(String duration){ this.duration = duration; }

    // getting distance
    public double getDistance(){
        return this.distance;
    }

    // setting distance
    public void setDistance(double distance){
        this.distance = distance;
    }

    // getting calorie
    public int getCalorie(){
        return this.calorie;
    }

    // setting calorie
    public void setCalorie(int calorie){
        this.calorie = calorie;
    }

    // getting frequency
    public int getFrequency(){
        return this.frequency;
    }

    // setting frequency
    public void setFrequency(int frequency){
        this.frequency = frequency;
    }
}
