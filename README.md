# Prove Auth Node

An authentication decision node that interacts with Prove trust and prefill capabilities.

## Compatibility

You can implement this node on the following systems:

| Product                               | Compatible? |
|---------------------------------------|-------------|
| ForgeRock Identity Cloud              | Yes         |
| ForgeRock Access Management (self-managed) | Yes    |
| ForgeRock Identity Platform (self-managed) | Yes    |

---

## Prove Trust Node

### Overview

Prove’s Trust Score is a real-time indicator of a phone number’s trustworthiness. It measures the real-time risk of authenticating a consumer using a specific phone number. Trust Score achieves this by analyzing and scoring daily life cycle change events and leveraging carrier signals and core telecom infrastructure signals. This enables Prove® to manage the tenure identity behind the phone number. Trust Score also augments possession and ownership indicators in other Prove APIs, completing the Possession-Reputation-Ownership (PRO) Model℠ to assist customers in detecting and understanding potential fraud risks.

### Inputs

This node requires the following inbound data:

| Description         | Attribute Name  | Source       |
|---------------------|-----------------|--------------|
| Telephone Number    | `telephoneNumber` | Shared state |

### Dependencies

To use this node, you must have already set up Identity Cloud integration with Prove.

### Configuration

The configurable properties for this node are:

| Property              | Usage                       |
|-----------------------|-----------------------------|
| `url`                 | Prove Trust API URL         |
| `tokenUrl`            | Prove Token URL             |
| `apiClientId`         | Prove API Client ID         |
| `proveUsername`       | Prove Username              |
| `provePassword`       | Prove Password              |
| `trustScore`          | Minimum Prove trust score   |
| `identifierSharedState` | Shared state key where the phone number is stored |

### Outputs

#### Outcomes

| Outcome  | Description                           |
|----------|---------------------------------------|
| `True`   | User is trustworthy                   |
| `False`  | User is not trustworthy               |
| `Error`  | An error message is output to the shared state |

---

## Prove Prefill Node

### Overview

The Prove Pre-Fill solution leverages the power of phones and phone numbers to modernize onboarding experiences. It delivers digital identities and data from trusted data providers, with consumer consent, to enhance application velocity while mitigating identity fraud.

### Inputs

This node requires the following inbound data:

| Description         | Attribute Name  | Source       |
|---------------------|-----------------|--------------|
| Telephone Number    | `userIdentifier` | Shared state |
| Date of Birth       | `proveDob`       | Shared state |

### Dependencies

To use this node, you must have already set up Identity Cloud integration with Prove.

### Configuration

The configurable properties for this node are:

| Property              | Usage                       |
|-----------------------|-----------------------------|
| `url`                 | Prove API URL              |
| `apiClientId`         | Prove API Client ID        |
| `subClientId`         | Prove Sub Client ID        |
| `proveUsername`       | Prove Username             |
| `provePassword`       | Prove Password             |
| `numberOfEmail`       | Prove Number of Emails     |
| `numberOfAddresses`   | Prove Number of Addresses  |
| `identifierSharedState` | Shared state key where the phone number is stored |

### Outputs

| Attribute          | Description                           |
|--------------------|---------------------------------------|
| `proveIndividual`  | The attributes of the user found by Prove |

#### Outcomes

| Outcome  | Description                           |
|----------|---------------------------------------|
| `True`   | Individual successfully identified for prefill |
| `False`  | Individual not found                  |
| `Error`  | An error message is output to the shared state |

---

<!-- SUPPORT -->

## Support

If you encounter any issues, be sure to check our **[Troubleshooting](https://backstage.forgerock.com/knowledge/kb/article/a68547609)** pages.

Support tickets can be raised whenever you need our assistance; here are some examples of when it is appropriate to open a ticket (but not limited to):

* Suspected bugs or problems with Ping Identity software.
* Requests for assistance 

You can raise a ticket using **[BackStage](https://backstage.forgerock.com/support/tickets)**, our customer support portal that provides one stop access to Ping Identity services.

BackStage shows all currently open support tickets and allows you to raise a new one by clicking **New Ticket**.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- COLLABORATION -->

## Contributing

This Ping Identity project does not accept third-party code submissions.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- LEGAL -->

## Disclaimer

> **This code is provided by Ping Identity on an “as is” basis, without warranty of any kind, to the fullest extent permitted by law.
>Ping Identity does not represent or warrant or make any guarantee regarding the use of this code or the accuracy,
>timeliness or completeness of any data or information relating to this code, and Ping Identity hereby disclaims all warranties whether express,
>or implied or statutory, including without limitation the implied warranties of merchantability, fitness for a particular purpose,
>and any warranty of non-infringement. Ping Identity shall not have any liability arising out of or related to any use,
>implementation or configuration of this code, including but not limited to use for any commercial purpose.
>Any action or suit relating to the use of the code may be brought only in the courts of a jurisdiction wherein
>Ping Identity resides or in which Ping Identity conducts its primary business, and under the laws of that jurisdiction excluding its conflict-of-law provisions.**

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- LICENSE - Links to the MIT LICENSE file in each repo. -->

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

&copy; Copyright 2024 Ping Identity. All Rights Reserved

[pingidentity-logo]: https://www.pingidentity.com/content/dam/picr/nav/Ping-Logo-2.svg "Ping Identity Logo"

## Troubleshooting

If this node logs an error, review the log messages for the transaction to find the reason for the exception.
