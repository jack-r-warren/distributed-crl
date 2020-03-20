interface Authority : Participant


interface Participant : Observer


interface Observer : Runnable {
  val servers: Collection<NetworkLocation>
}

