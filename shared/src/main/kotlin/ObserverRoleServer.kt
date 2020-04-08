open class ObserverRoleServer(otherServers: MutableMap<NetworkIdentity, SocketTuple>) : ProtocolServer(otherServers) {
  override fun handleMessage(identity: NetworkIdentity, message: Dcrl.BlockchainRequest): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }

  override fun handleMessage(identity: NetworkIdentity, message: Dcrl.BlockRequest): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.ErrorMessage,
    from: Dcrl.Certificate?
  ): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.CertificateRevocation,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.BlockMessage,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.BlockchainResponse,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.BlockResponse,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.Announce,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage? {
    TODO("Not yet implemented")
  }
}