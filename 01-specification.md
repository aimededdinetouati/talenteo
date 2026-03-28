This exercise is designed to provide a clear assessment of the candidate's ability to manage feature implementation in a structured and effective manner.

Task 1 - Employee Management Module

Objective

Design and implement an Employee Management module for an HR system using Spring Boot.

The system should allow HR to manage employees and maintain their core information.

Business Context

An employee in the system represents a person hired by the company.

Core information typically includes:
Personal information (e.g., first name, last name, email)
Employment details (e.g., hire date, department, position)
Compensation data (e.g., base salary, optional bonuses)

You are free to decide:
The exact data structure
Field constraints and validations
How the API should be designed


Expected Capabilities

The system should allow:
Creating new employees
Retrieving employee details
Updating employee information
Removing or deactivating employees


Task 2 - Payroll Feature

Objective

Extend the HR system by implementing a Payroll feature.

Payroll represents the monthly salary calculation for an employee.

It may include:
Base salary
Bonuses (optional)
Deductions (taxes, contributions)
Final net salary


Business Context
You may implement rules such as:
Taxes and deductions are applied to the base salary
Additional rules may apply for higher salary brackets
Payroll should be calculated consistently for a given period
Payroll results should not change retroactively once finalized


Expected Capabilities

Calculate payroll for a given employee
Retrieve payroll details for a specific period (e.g., month/year)
List payroll history for an employee



What We Are Evaluating
Data modeling decisions
REST API design
Validation strategy
Proper layering (Controller / Service / Repository)
Error handling
Code clarity and maintainability
Test coverage

Document any assumptions you make

