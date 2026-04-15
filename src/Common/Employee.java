package Common;
import java.io.Serializable;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String employeeId;
    private String firstName;
    private String lastName;
    private String icPassport;
    
    private String phoneNo;
    
    private String emergencyName;
    private String emergencyPhoneNo;
    private String emergencyRelationship;

    
    private int leaveBalance =15;
    
    public Employee(){}
    
    public Employee(String firstName, String lastName, String icPassport){
        this.firstName = firstName;
        this.lastName = lastName;
        this.icPassport = icPassport;
    }
    
    public String getEmployeeId(){
        return employeeId;
    }
    public String getFirstName(){
        return firstName;
    }
    public String getLastName(){
        return lastName;
    }
    public String getIcPassport(){
        return icPassport;
    }
    public String getPhoneNo(){
        return phoneNo;
    }
    public String getEmergencyName(){
        return emergencyName;
    }
    public String getEmergencyNo(){
        return emergencyPhoneNo;
    }
    public String getEmergencyRelationship(){
        return emergencyRelationship;
    }
    public int getLeaveBalance(){
        return leaveBalance;
    }
    
    public void setEmployeeId(String employeeId){
        this.employeeId = employeeId;
    }
    public void setFirstName(String firstName){
        this.firstName = firstName;
    }
    public void setLastName(String lastName){
        this.lastName = lastName;
    }
    public void setIcPassport(String icPassport){
        this.icPassport = icPassport;
    }
    public void setPhoneNo(String phoneNo){
        this.phoneNo = phoneNo;
    }
    public void setEmergencyName(String emergencyName){
        this.emergencyName = emergencyName;
    }
    public void setEmergencyPhoneNo(String emergencyPhoneNo){
        this.emergencyPhoneNo = emergencyPhoneNo;
    }
    public void setEmergencyRelationship(String emergencyRelationship){
        this.emergencyRelationship = emergencyRelationship;
    }
    public void setLeaveBalance(int leaveBalance){
        this.leaveBalance = leaveBalance;
    }
}
