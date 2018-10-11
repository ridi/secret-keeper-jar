import com.ridi.secretkeeper.SecretKeeper

object Sample extends App {
  val secret = SecretKeeper.tell("sample.secret1")
  println(secret)
}
