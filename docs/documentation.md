# Documentation

## Design Choices

- Authorities are hard-coded since Authority status is typically gained and revoked out-of-band.
- Participant status is given to those with a Participant Certificate signed by an Authority.
- DCRLMessages are either signed or unsigned.
Another approach would be to have every message signed, and messages that should not be signed will not have the necessary fields.
It would be the receiver's responsibility to valid a signature (if it exists) and *whether a signature is necessary for the message*.
- BlockRequests are unsigned, since an identity does not need to be proven.
- All messages are unencrypted since confidentiality is not necessary to provide.
- We will want to mention that the Discovery Server is for our proof of concept. Since the messages from the Discovery Server are not signed, there is a possibility that an attacker would edit who is an Authority. Then a self-signed Authority Certificate would be accepted. I think that the Discovery Server will have to say who is an Authority and who is a Participant. Otherwise, the Participants will need to have a hard-coded list of Authorities. An attack could be a Participant self-signs an Authority Certificate.

## Data Structures

### Authority

An Authority consists of the following:

- A Certificate which is self-signed
- Everything a Participant consists of

In addition, an Authority needs to perform the following:

- Revoke a Participant's certificate
- Send a CertificateRevocation to Participants
- Everything a Participant needs to perform

### Participant

A Participant consists of the following:

- A Certificate which is signed by an Authority authorizing the party to participate in the network
- A list of CertificateRevocations to process
- The Blockchain
- The hash and height of the most recent validated Block (for efficiency when validating new Blocks)
- Everything a Non-Participant consists of

In addition, a Participant needs to perform the following:

- Receive a CertificateRevocation from an Authority
- Send a Block to all other Participants
- Receive a Block from a Participant
- Request a specific Block by its height
- Resolve forks
- Respond to requests from Non-Participants
- Everything a Non-Participant needs to perform

### Non-Participants

A Non-Participant consists of the following:

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

- AUTHORITY granting the ability to (1) sign CertificateRevocations and (2) sign Participant Cerificates
- PARTICIPATION granting the ability to (1) create and sign new blocks for the blockchain and (2) serve Non-Participants the blockchain

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

1. A party (Authority, Participant, Non-Participant) communicates with the Discovery Server to obtain a list of all Authorities and Participants.

### Revocation Process

1. The Authority sends the CertificateRevocation (that it receives out-of-band) to all Participants.
1. A Participant receives the CertificateRevocation and adds it to its list of CertificateRevocations.
1. Once a Participant has enough CertificateRevocations to form a Block, it does so and sends that Block to all Authorities and all other Participants.
1. The Authorities and other Participants will accept the Block and add it to their Blockchain.

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
