package stablecoin.v5

import org.scalatest.PropSpec
import stablecoin.v5.UpdateContract.address

class UpdateContractSpec extends PropSpec {

  property("Update Address check") {
    println("UpdateAddress: " + address)
    assert(
      address == "2TPQdXG8PSPPPPMGtSM6aDTdjH98ygmvuGthWocQkmGJ6Pvxo1KPbkJSEjHsxPLjychozgjcQuzXUTGyq9wQ4WnBMPnpAUz4Bdi7c5p8FKfnQRwvkAuLx9xyk7VCyqvGAAbLmVgXBy3Mt4CSswLByXqqYkkg8mHFQHpKpJLpi7gofxjLexN35xkSqoxk9rfjUT4UZiP7wxqvFPz5czv5K1XMaMLQkdMGGZ5e7t8LkDdRUPaQMnyEyQbnnCqc3kFZRgWZmkG4dmZmx7VnSi1EwdZbJY7ufVduc9SjZhhP6oaNz4cUUbLJ7i5EEFy7GSp8FsMwbCjyr8avTHYdapipZY98CMZNMiqXPSf8WZDrG3KzrVFFVFoDRNANNMrqwwyBhfUGQMHxMuho5jpbbZzgk8BwF45Lyeqkzi747Swy6S315D")
  }
}
