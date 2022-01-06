/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.spine.server.aggregate.given.salary;

import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.given.command.DecreaseSalary;
import io.spine.server.aggregate.given.command.Employ;
import io.spine.server.aggregate.given.command.IncreaseSalary;
import io.spine.server.aggregate.given.command.ShakeUpSalary;
import io.spine.server.aggregate.given.dispatch.AggregateMessageDispatcher;
import io.spine.server.aggregate.given.salary.event.NewEmployed;
import io.spine.server.aggregate.given.salary.event.SalaryDecreased;
import io.spine.server.aggregate.given.salary.event.SalaryIncreased;
import io.spine.server.command.Assign;

import java.util.List;

import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.env;
import static io.spine.server.aggregate.given.salary.Employees.salaryDecreased;
import static io.spine.server.aggregate.given.salary.Employees.salaryIncreased;

public class EmployeeAgg extends Aggregate<EmployeeId, Employee, Employee.Builder> {

    public EmployeeAgg(EmployeeId id) {
        super(id);
    }

    @Assign
    NewEmployed handle(Employ cmd) {
        return NewEmployed.newBuilder()
                .setEmployee(cmd.getEmployee())
                .setSalary(cmd.getSalary())
                .vBuild();
    }

    @Apply
    private void on(NewEmployed event) {
        builder()
                .setId(event.getEmployee())
                .setSalary(event.getSalary());
    }

    @Assign
    Iterable<EventMessage> handle(ShakeUpSalary cmd) {
        var employee = cmd.getEmployee();
        return List.of(
                salaryDecreased(employee, 15),
                salaryIncreased(employee, 50),
                salaryIncreased(employee, 75),

                // this one would make the aggregate's state invalid.
                // employee can not be paid less than 200.
                salaryDecreased(employee, 1000),

                salaryIncreased(employee, 100),
                salaryIncreased(employee, 75),
                salaryIncreased(employee, 50)
        );
    }

    @Assign
    SalaryIncreased handle(IncreaseSalary cmd) {
        return salaryIncreased(cmd.getEmployee(), cmd.getAmount());
    }

    @Apply
    private void on(SalaryIncreased event) {
        builder()
                .setId(event.getEmployee())
                .setSalary(state().getSalary() + event.getAmount());
    }

    @Assign
    SalaryDecreased handle(DecreaseSalary cmd) {
        return salaryDecreased(cmd.getEmployee(), cmd.getAmount());
    }

    @Apply
    private void on(SalaryDecreased event) {
        builder()
                .setId(event.getEmployee())
                .setSalary(state().getSalary() - event.getAmount());
    }

    public void dispatchCommands(CommandMessage... commands) {
        for (var cmd : commands) {
            AggregateMessageDispatcher.dispatchCommand(this, env(cmd));
        }
    }
}
