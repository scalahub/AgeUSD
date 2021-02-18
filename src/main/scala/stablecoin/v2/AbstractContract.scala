package stablecoin.v2

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.ergo.KioskType

private[v2] abstract class AbstractContract {
  import scala.collection.mutable.{Map => MMap}
  val env = MMap[String, KioskType[_]]()

  val script: String
  lazy val ergoTree = kiosk.script.ScriptUtil.compile(env.toMap, script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
}
