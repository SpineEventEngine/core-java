/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.server.aggregate.given.salary.command.DecreaseSalary;
import io.spine.server.aggregate.given.salary.command.DecreaseSalaryThreeTimes;
import io.spine.server.aggregate.given.salary.command.Employ;
import io.spine.server.aggregate.given.salary.command.IncreaseSalary;
import io.spine.server.aggregate.given.salary.event.SalaryDecreased;

public class Employees {

    private Employees() {
    }

    public static EmployeeId newEmployee() {
        return EmployeeId.generate();
    }

    public static Employ employ(EmployeeId employee, int salary) {
        return Employ.newBuilder()
                .setEmployee(employee)
                .setSalary(salary)
                .vBuild();
    }

    public static IncreaseSalary increaseSalary(EmployeeId employee, int amount) {
        return IncreaseSalary.newBuilder()
                .setEmployee(employee)
                .setAmount(amount)
                .vBuild();
    }

    public static DecreaseSalary decreaseSalary(EmployeeId employee, int amount) {
        return DecreaseSalary.newBuilder()
                .setEmployee(employee)
                .setAmount(amount)
                .vBuild();
    }

    public static DecreaseSalaryThreeTimes
    decreaseSalaryThreeTimes(EmployeeId employee, int amount) {
        return DecreaseSalaryThreeTimes.newBuilder()
                .setEmployee(employee)
                .setAmount(amount)
                .vBuild();
    }

    static SalaryDecreased salaryDecreased(EmployeeId employee, int amount) {
        return SalaryDecreased.newBuilder()
                .setEmployee(employee)
                .setAmount(amount)
                .vBuild();
    }
}
