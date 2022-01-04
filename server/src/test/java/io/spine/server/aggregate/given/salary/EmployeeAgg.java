package io.spine.server.aggregate.given.salary;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.given.command.DecreaseSalary;
import io.spine.server.aggregate.given.command.IncreaseSalary;
import io.spine.server.aggregate.given.salary.event.SalaryDecreased;
import io.spine.server.aggregate.given.salary.event.SalaryIncreased;
import io.spine.server.command.Assign;

public class EmployeeAgg extends Aggregate<EmployeeId, Employee, Employee.Builder> {

    @Assign
    SalaryIncreased handle(IncreaseSalary cmd) {
        return SalaryIncreased.newBuilder()
                .setEmployee(cmd.getEmployee())
                .setAmount(cmd.getAmount())
                .vBuild();
    }

    @Apply
    void on(SalaryIncreased event) {
        builder()
                .setId(event.getEmployee())
                .setSalary(state().getSalary() + event.getAmount());
    }

    @Assign
    SalaryDecreased handle(DecreaseSalary cmd) {
        return SalaryDecreased.newBuilder()
                .setEmployee(cmd.getEmployee())
                .setAmount(cmd.getAmount())
                .vBuild();
    }

    @Apply
    void on(SalaryDecreased event) {
        builder()
                .setId(event.getEmployee())
                .setSalary(state().getSalary() - event.getAmount());
    }
}
