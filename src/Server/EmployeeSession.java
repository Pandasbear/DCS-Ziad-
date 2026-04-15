package Server;

import Common.Employee;
import java.util.concurrent.ConcurrentHashMap;
public class EmployeeSession {
    private final ConcurrentHashMap<String, Employee> map = new ConcurrentHashMap<>();
    
    public void put(Employee employees){map.put(employees.getEmployeeId(), employees);}
    public Employee get(String employeeId){
        return map.get(employeeId);
    }
}
    
