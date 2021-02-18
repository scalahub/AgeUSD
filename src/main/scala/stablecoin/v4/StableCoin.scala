package stablecoin.v4

import Contracts._

import scala.util.Random

object StableCoin {

  type BaseCurrency = Long // NanoErgs
  type StableCoins = Long
  type ReserveCoins = Long
  type Rate = Long

  val minReserveRatioPercent: Long = 400
  val maxReserveRatioPercent: Long = 800

  val rcDefaultPrice = 1000000L

  val INF = Long.MaxValue

  private def getOracleRate: Rate = 1000 + Random.nextInt(100) // NanoErgs per Usd (to convert to this format from real oracle)

  // sc must be in units eUsd

  def getBcDeltaWithFee(bcDelta: Long) = {
    val fee = bcDelta * feePercent / 100
    bcDelta + (if (fee < 0) -fee else fee)
  }

  def getDeltaBcForDeltaSC(bcReserveIn: BaseCurrency, scCircIn: StableCoins, scCircDelta: StableCoins, rate: Rate): BaseCurrency = {
    require(scCircDelta != 0)

    val bcReserveNeededIn = rate * scCircIn
    val liabilitiesIn = bcReserveIn.min(bcReserveNeededIn)

    val liableRate = if (scCircIn == 0) INF else liabilitiesIn / scCircIn
    val scNominalPrice = rate.min(liableRate)

    val bcReserveDelta = scCircDelta * scNominalPrice
    val bcReserveOut = bcReserveIn + bcReserveDelta
    val scCircOut = scCircIn + scCircDelta

    val bcReserveNeededOut = rate * scCircOut

    val reserveRatioPercentOut =
      if (bcReserveNeededOut == 0)
        maxReserveRatioPercent
      else
        bcReserveOut * 100 / bcReserveNeededOut

    require(scCircOut >= 0, s"Error: scCircOut ($scCircOut) < 0")
    require(bcReserveOut >= 0, s"Error: bcReserveOut ($bcReserveOut) < 0")
    println(s"scNominalPrice $scNominalPrice")
    println(s"bcReserveIn $bcReserveIn")
    println(s"bcReserveOut $bcReserveOut")
    println(s"bcReserveNeededIn $bcReserveNeededIn")
    println(s"bcReserveNeededOut $bcReserveNeededOut")
    println(s"bcReserveDelta $bcReserveDelta")
    println(s"reserveRatioPercentOut $reserveRatioPercentOut")
    if (scCircDelta > 0) {
      require(reserveRatioPercentOut >= minReserveRatioPercent, s"reserveRatioPercentOut ($reserveRatioPercentOut) < minReserveRatioPercent ($minReserveRatioPercent)")
    }
    getBcDeltaWithFee(bcReserveDelta)
  }

  def getDeltaBcForDeltaRC(bcReserveIn: BaseCurrency, scCircIn: StableCoins, rcCircIn: ReserveCoins, rcCircDelta: ReserveCoins, rate: Rate): BaseCurrency = {
    require(rcCircDelta != 0)
    val bcReserveNeededIn = rate * scCircIn
    val liabilitiesIn = bcReserveIn.min(bcReserveNeededIn)

    val equityIn = bcReserveIn - liabilitiesIn
    val equityRate = if (rcCircIn == 0) rcDefaultPrice else equityIn / rcCircIn
    val rcNominalPrice = if (equityIn == 0) rcDefaultPrice else equityRate

    val bcReserveDelta = rcNominalPrice * rcCircDelta
    println(s"bcReserveIn $bcReserveIn")
    println(s"bcReserveNeededIn $bcReserveNeededIn")
    println(s"liabilitiesIn $liabilitiesIn")
    println(s"equityIn $equityIn")
    println(s"equityRate $equityRate")
    println(s"rcNominalPrice $rcNominalPrice")

    val bcReserveOut = bcReserveIn + bcReserveDelta
    val rcCircOut = rcCircIn + rcCircDelta

    val bcReserveNeededOut = bcReserveNeededIn // scCircIn == scCircOut because this is rc exchange

    val reserveRatioPercentOut =
      if (bcReserveNeededOut == 0)
        maxReserveRatioPercent
      else
        bcReserveOut * 100 / bcReserveNeededOut

    require(rcCircOut >= 0, s"Error: scCircOut ($rcCircOut) < 0")
    require(bcReserveOut >= 0, s"Error: bcReserveOut ($bcReserveOut) < 0")
    if (rcCircDelta > 0) { // minting
      require(reserveRatioPercentOut <= maxReserveRatioPercent, s"reserveRatioPercentOut ($reserveRatioPercentOut) > maxReserveRatioPercent ($maxReserveRatioPercent)")
      // minting RC should be allowed as long as (1) out-reserve-ratio is below max and (2) out-reserve-ratio > in-reserve-ratio
    } else { // redeeming
      require(reserveRatioPercentOut >= minReserveRatioPercent)
      // redeeming RC should be allowed only if final reserve ratio is within limits
    }

    getBcDeltaWithFee(bcReserveDelta)
  }

  var bcReserve = 0L
  var rcCirc = 0L
  var scCirc = 0L

  def testRcExchange: Unit = {
    val rcCircDeltas = Seq(11, 1, -4, -5, 300, -2, 4000, -30, 50, -60, 10000)
    rcCircDeltas foreach { rcCircDelta =>
      println(f"Before. SC: $scCirc%10d, RC: $rcCirc%10d, BR: $bcReserve%10d")
      val bcReserveDelta = getDeltaBcForDeltaRC(bcReserve, scCirc, rcCirc, rcCircDelta, getOracleRate)
      println(f" Delta. SC: ${0}%10d, RC: $rcCircDelta%10d, BR: $bcReserveDelta%10d")
      bcReserve += bcReserveDelta
      rcCirc += rcCircDelta
      println(f" After. SC: $scCirc%10d, RC: $rcCirc%10d, BR: $bcReserve%10d")
      println("------------------------------------------------------")
    }
  }

  def testScExchange: Unit = {
    val scCircDeltas = Seq(11, 1, -4, -5, 300, -2, 400, -30, 50, -60)
    scCircDeltas foreach { scCircDelta =>
      println(f"Before. SC: $scCirc%10d, RC: $rcCirc%10d, BR: $bcReserve%10d")
      val bcReserveDelta = getDeltaBcForDeltaSC(bcReserve, scCirc, scCircDelta, getOracleRate)
      println(f" Delta. SC: $scCircDelta%10d, RC: ${0}%10d, BR: $bcReserveDelta%10d")
      bcReserve += bcReserveDelta
      scCirc += scCircDelta
      println(f" After. SC: $scCirc%10d, RC: $rcCirc%10d, BR: $bcReserve%10d")
      println("------------------------------------------------------")
    }
  }

  def main(args: Array[String]): Unit = {
    testRcExchange
    testScExchange
  }
}
