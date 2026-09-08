# Database-Workload-Firebreak

A middleware-based system designed to provide performance isolation and fair resource sharing among multiple tenants accessing a shared database.

## Problem

In a shared database environment, one application or tenant can generate excessive or resource-intensive requests. This can consume a large number of available resources and negatively affect the performance of other applications.

This project introduces a workload governance layer that manages requests before they reach the shared database.

## Objectives

- Provide performance isolation between tenants.
- Ensure fair resource sharing.
- Control and manage database workloads.
- Monitor workload and system performance.

## Proposed Architecture

```text
Client Applications
        ↓
Database Workload Firebreak
        ↓
Tenant Identification
        ↓
Workload Classification
        ↓
Policy Engine
        ↓
Admission Controller
        ↓
Per-Tenant Queues
        ↓
Weighted Scheduler
        ↓
Worker & Connection Manager
        ↓
Shared Database
