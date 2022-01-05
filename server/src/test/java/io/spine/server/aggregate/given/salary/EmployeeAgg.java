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
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.DefaultAggregateRepository;
import io.spine.server.aggregate.given.command.Employ;
import io.spine.server.aggregate.given.command.ShakeUpSalary;
import io.spine.server.aggregate.given.dispatch.AggregateMessageDispatcher;
import io.spine.server.aggregate.given.salary.event.NewEmployed;
import io.spine.server.aggregate.given.salary.event.SalaryDecreased;
import io.spine.server.aggregate.given.salary.event.SalaryIncreased;
import io.spine.server.command.Assign;
import io.spine.testing.server.blackbox.BlackBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.env;
import static io.spine.server.aggregate.given.salary.Employees.employ;
import static io.spine.server.aggregate.given.salary.Employees.newEmployee;
import static io.spine.server.aggregate.given.salary.Employees.shakeUpSalary;

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

    @Apply
    private void on(SalaryIncreased event) {
        builder()
                .setId(event.getEmployee())
                .setSalary(state().getSalary() + event.getAmount());
    }

    @Apply
    private void on(SalaryDecreased event) {
        builder()
                .setId(event.getEmployee())
                .setSalary(state().getSalary() - event.getAmount());
    }

    @Assign
    Iterable<EventMessage> handle(ShakeUpSalary cmd) {
        var employee = cmd.getEmployee();
        return List.of(

                // we need several events to make them processed as a batch.
                salaryDecreased(employee, 15),
                salaryIncreased(employee, 50),
                salaryIncreased(employee, 75),
                salaryIncreased(employee, 100),

                // this one would make the aggregate's state invalid.
                // employee's can not be paid less than 200.
                salaryDecreased(employee, 1000)
        );
    }

    private static SalaryIncreased salaryIncreased(EmployeeId employee, int amount) {
        return SalaryIncreased.newBuilder()
                .setEmployee(employee)
                .setAmount(amount)
                .vBuild();
    }

    private static SalaryDecreased salaryDecreased(EmployeeId employee, int amount) {
        return SalaryDecreased.newBuilder()
                .setEmployee(employee)
                .setAmount(amount)
                .vBuild();
    }

    public void dispatchCommands(CommandMessage... commands) {
        for (var cmd : commands) {
            AggregateMessageDispatcher.dispatchCommand(this, env(cmd));
        }
    }
}
