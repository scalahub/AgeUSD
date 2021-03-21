package stablecoin.v5

import org.scalatest.PropSpec
import stablecoin.v5.UpdateContract.address

class UpdateContractSpec extends PropSpec {

  property("Update Address check") {
    println("UpdateAddress: " + address)
    assert(
      address == "2TPQdXG8PSPPPPMGtSM6S36t2JXdDGZ9rjC6QFfgHYQMbeoD1CaD3qrqNej7E7kc8ogqECL9zhWC3FiUmuvyUd5khmCrdZ8wtahiLB3o7DierY5g2u3kMoNj58hgHvfWnvpSTb7dZnC5aEMt8ri6X3JYriv7GknpUsHqLhzYUDQnuQKYuvhZBeALytmGrLpLK9QCgy4ESyJB72iFVcp63neKzign6XTLDvQ3RavVYkANEj5RtwSXkTvypREHeNAm9YE1KTXfRZ1GPnDJRzaVozQi1o1QFrf9NoD5yprhTUwsmqMtsjdRfpVwbYSAkij7bLQz87XRpMp1FnVvrhkyzxGyiNbTP4fYz1Dn6BeU3iuoohJL9fVHYGDR8YNp33NfxXNpdAvuQ3jYTrPj3sefQ756JWbmEjKRTMNbtd45aLc9nt")
  }
}
