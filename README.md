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

## Troubleshooting

If this node logs an error, review the log messages for the transaction to find the reason for the exception.
