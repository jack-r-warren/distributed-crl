# Documentation

## Design Choices

- Authorities are hard-coded since Authority status is typically gained and revoked out-of-band.
- Participants are hard-coded for simplicity.
However, there is the infrastructure to revoke Participant status.
- DCRLMessages are either signed or unsigned.
Another approach would be to have every message signed, and messages that should not be signed will not have the necessary fields.
It would be the receiver's responsibility to valid a signature (if it exists) and *whether a signature is necessary for the message*.
- BlockRequests are unsigned, since an identity does not need to be proven.
- All messages are unencrypted since confidentiality is not necessary to provide.

## Data Structures

### Authority

An Authority consists of the following:

 - Certificates for signing messages and authorizing participation (A single cert may have both usages, but is not preferred)
- A list of Participants it has authorized
- Everything a Participant consists of

In addition, an Authority needs to perform the following:

- Add a Participant to the network (?)
- Revoke a Participant's certificate
- Receive a CertificateRevocation
- Send a CertificateRevocation to Participants
- Everything a Participant needs to perform

### Participant

A Participant consists of the following:

- A Certificate
- A list of all Authorities
- A list of all other Participants
- A list of CertificateRevocations to process
- The Blockchain
- The hash and height of the most recent validated Block (for efficiency when validating new Blocks)
- Everything a Non-Participant consists of

In addition, a Participant needs to perform the following:

- Request Participant status (?)
- Process the addition of a Participant (?)
- Process the revocation of a Participant
- Receive a CertificateRevocation from an Authority
- Send a Block to all other Participants
- Receive a Block from a Participant
- Request a specific Block by its height
- Resolve forks
- Respond to requests from Non-Participants
- Everything a Non-Participant needs to perform

### Non-Participants

A Non-Participant consists of the following:

- A list of Participants to contact

In addition, a Non-Participant needs to perform the following:

- Request a copy of the Blockchain

### Blockchain

A Blockchain consists of the following:

- A list of Blocks

### Block

A Block consists of the following:

- The Certificate of the Participant who created the Block
- It's height
- The hash of the previous Block
- A timestamp
- The Merkle root of its CertificateRevocations
- A list of CertificateRevocations

### Certificate

A Certificate consists of the following:

- A subject
- A valid start time and length
- A list of CertificateUsages
- A signing public key
- The hash of the issuer's Certificate
- The issuer's signature of this Certificate

### CertificateUsage

A CertificateUsage is one of the following:

- AUTHORITY
- PARTICIPATION

Authorities should have both usages if they wish to be a Participant.

## Messages

### DCRLMessage

One of the established messages in the protocol:

- ParticipantRequest
- ParticipantResponse
- ParticipantIntroduction
- CertificateRevocation
- BlockchainRequest
- BlockRequest
- ErrorMessage
- SignedMessage

### ParticipantRequest

**Do we need this?**

A message from a Participant to an Authority requesting Participant status in the network.

### ParticipantResponse

**Do we need this?**

A message from an Authority to a Participant containing the Authority's signature of the Participant's Certificate.

### ParticipantIntroduction

**Do we need this?**

A message from a Participant to a Participant telling the receiver to add the sender to their list of Participants.

### CertificateRevocation

A message containing a Certificate to be revoked.
This message originally is sent to an Authority and is forwarded to Participants.
This message is one of message 'transactions' in a Block.

### BlockchainRequest

A message to a Participant requesting the entire Blockchain.
Since this message can came from Non-Participants, it must be unsigned.

### BlockRequest

A message to a Participant requesting a specific Block by its height.
This message is unsigned since there is no need to prove an identity.

### ErrorMessage

**Should we make this message either signed or unsigned?**
**Are there use cases for this message some times being signed and other times being unsigned?**

A message containing an error message.

### SignedMessage

**We can choose to hash the message content and then sign or concatenate the bytes and sign those.**

A message containing the sender's Certificate and signature of the message content.

### BlockMessage

A message from a Participant to a Participant containing the necessary information for a Block.
The receiver will add the Block to their Blockchain if the Block is valid.

### BlockchainResponse

A message from a Participant containing all Blocks in the sender's Blockchain.

### BlockResponse

A message from a Participant containing the Block at the requested height.

## Processes

### Bootstrapping Process

1. A Participant asks an Authority for Participant status
1. The Authority authorizes the Participant to act as a Participant by signing the Participant's certificate and giving the Participant lists of all Authorities and Participants
1. The Participant asks all other Participants to add itself to their list of Participants

### Revocation Process

1. An Authority receives a CertificateRevocation.
1. The Authority sends the CertificateRevocation to all Participants.
1. A Participant receives the CertificateRevocation and adds it to its list of CertificateRevocations.
1. Once a Participant has enough CertificateRevocations to form a Block, it does so and sends that Block to all other Participants.
1. The other Participants will accept the Block and add it to their Blockchain.

### Block Validation Process

1. Validate the Certificate.
(It should be the same Certificate which was validated in the SignedMessage)
1. If this Block's height is greater than 1 plus the height of the Participant's last validated Block, then the Participant will need to perform Fork Resolution.
1. Compute the previous Block's hash and compare it with this Block's hash.
Hashes are computed over the Certificate, height, previous Block's hash, timestamp, and Merkle root.
1. Compute the previous Block's Merkle root and compare it with this Block's Merkle root.

### Fork Resolution Process

1. The Participant will accept the longer Blockchain.
1. To validate the longer Blockchain, the Participant will request missing Blocks in decreasing order until there is a Block common in the Participant's Blockchain. If all of the missing Blocks are valid, then the longer chain will be accepted.
