# MyTailor

A tailoring shop management application designed to help tailors organize customer information, measurements, orders, sewing tasks, delivery dates, and payments.

> **Project status:** The GitHub repository is currently empty. This README describes the project's intended direction and planned functionality; it does not claim that these features are already implemented.

## Project goals

MyTailor aims to provide a simple way for an individual tailor or a tailoring shop to manage day-to-day work in one place, with a user interface suitable for Dari/Persian-speaking users.

## Planned features

- **Customer management** — save customer names and contact information.
- **Customer measurements** — record and retrieve body measurements for future orders.
- **Order management** — record clothing orders, garment details, order dates, prices, and expected delivery dates.
- **Work-task assignment** — assign tasks for an order, such as cutting, sewing, finishing, and ironing, to the appropriate employees.
- **Order progress** — track work from registration through completion and delivery.
- **Payments and balances** — record deposits, additional payments, and outstanding balances.
- **Employee management** — maintain employee details and track assigned work.
- **Flexible compensation** — support configurable daily, weekly, monthly, or per-task pay rates, with task-based wages calculated from completed work.
- **Dari/Persian and RTL interface** — support a right-to-left interface for Persian-language users.
- **Online accounts and backups (future direction)** — allow users to access their own shop data securely from multiple devices, with a backup strategy.

These are target features and may require implementation before they are available in the application.

## Intended workflow

1. Register a customer and save their measurements.
2. Create a clothing order with the garment details, price, and delivery date.
3. Add the required work tasks to the order and assign them to employees.
4. Update each task as work progresses.
5. Record payments and review the remaining balance.
6. Complete the order and record its delivery.

## Technology

The exact technology stack has not yet been documented. Update this section to match the actual project implementation, for example:

- Android application: Kotlin or Java
- User interface: native Android UI or the framework selected by the project
- Local storage: the database selected by the project
- Optional online service: an authenticated backend and managed database

Do not treat the examples above as confirmed dependencies.

## Getting started

The repository does not currently contain application source files, so setup and run instructions cannot be provided yet.

Once the project code has been added:

1. Clone the repository:
   ```bash
   git clone https://github.com/ams-mas/mytailor.git
   cd mytailor
   ```
2. Open the project using the development environment required by its technology stack.
3. Follow the project's build and configuration instructions, which should be added here when available.

## Data security and privacy

Customer contact details and body measurements can be sensitive. Before using real customer information, the application should:

- restrict access to authorized users;
- avoid storing passwords or API secrets in source code;
- protect data in transit and, where appropriate, at rest;
- provide reliable backups and test data restoration;
- clearly explain how data is stored, synchronized, and deleted.

Online synchronization and automatic cloud backup should be documented only after they are implemented and tested.

## Contributing

Development and feature improvements are welcome. Before submitting changes, describe the purpose of the change and test it with the project's documented build process.

## License

No license has been specified yet. Until a license is added, the repository should not be assumed to grant permission to reuse, modify, or redistribute its code.

## Repository

https://github.com/ams-mas/mytailor
