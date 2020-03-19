# Documentation

## Data Structures

### Certificate Authority

A Certificate Authority consists of the following:

- A Certificate
- A list of Intermediaries it has authorized
- A list of revocations to process

In addition, a Certificate Authority needs to perform the following:

- Sign an Intermediary's certificate
- Revoke an Intermediary's certificate
- Receive a Revocation
- Send a Revocation to Intermediaries

### Intermediary

An Intermediary consists of the following:

- A Certificate
- A list of all Certificate Authorities
- A list of all other Intermediaries
- A list of Revocations to process
- The Blockchain
- The hash and height of the most recent validated Block (for efficiency when validating new Blocks)

In addition, an Intermediary needs to perform the following:

- Bootstrap to get a list of all other Intermediaries and the Blockchain
- Respond to an Intermediary trying to bootstrap
- Process an Intermediary being revoked
- Receive a Revocation from its Certificate Authority
- Send a Block to all other Intermediaries
- Receive a Block from an Intermediary
- Request a specific Block by its height and hash
- Resolve forks
- Respond to requests from Non-Participants

### Non-Participants

A Non-Participant consists of the following:

- A list of Intermediaries to contact

In addition, a Non-Participant needs to perform the following:

- Request a copy of the Blockchain

### Revocation

TODO

### Certificate

TODO

### Blockchain

A Blockchain consists of the following:

- A list of Blocks

### Block

A Block consists of the following:

- The Certificate of the Intermediary who created the Block
- It's height
- The hash of the previous Block
- The Merkle root of its Revocations
- A list of Revocations

## Bootstrapping Process

1. A Certificate Authority authorizes a party to act as an Intermediary by siging the Intermediary's certificate and giving the Intermediary a list of the Certificate Authority's other Intermediaries.
1. The Intermediary contacts its Certificate Authorities's other Intermediaries for a list of all other Intermediaries.
1. The Intermediary asks all other Intermediaries to add itself to their list of Intermediaries

## Revocation Process

1. A Certificate Authority receives a Revocation.
1. The Certificate Authority sends the Revocation to its Intermediaries.
1. An Intermediary receives the Revocation and adds it to its list of Revocations.
1. Once an Intermediary has enough Revocations to form a Block, it does so and sends that Block to all other Intermediaries.
1. The other Intermediaries will accept the Block and add it to their Blockchain.

## Fork Resolution Process

1. The Intermediary will accept the longer Blockchain.
1. To validate the longer Blockchain, the Intermediary will request missing Blocks in decreasing order until there is a Block common in the Intermediary's Blockchain. If all of the missing Blocks are valid, then the longer chain will be accepted.
