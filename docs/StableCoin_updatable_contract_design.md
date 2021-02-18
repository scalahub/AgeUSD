### Updatable StableCoin Protocol
Consider the "Bank" box in exchange

Since a "contract" in our terminology implies the ErgoTree code, while our update requirements are much more, we will instead
use the term "protocol" to refer to the thing we want to update.

In the context of UTXOs and specifically Ergo, we can think of updating the protocol as a combination of one or more of the following (note that some combinations are mutually exclusive)
The options selected for our StableCoin design are given in **bold**.
1. **Update the script (address) of the bank box**
2. Update one or more registers of the bank box
3. Update one or more tokens of the bank box
4. Preserve the box id of the bank box
5. Preserve the script (address) of the bank box
6. **Preserve the registers of the bank box**
7. **Preserve the tokens of the bank box**

### Protocol Voting box

We will have a voting mechanism to decide when the protocol will be updated. At a high level, this protocol update box 
will contain an NFT that will be referred to in the bank box. The register R4 of the voting box will contain the script of the 
new bank box. Based on the level of flexibility and complexity, we can also
classify how the protocol voting box can be updated:

1. Allow voters to also update the script (address) of the Voting box
2. **Do not allow voters to update the script of the voting box** 

### Voting Mechanism

Before describing our update protocol, we will first describe the underlying voting protocol used.
This is a generic primitive for many other applications because it is not coupled with the application.

The voting protocol at a high level works as follows. The vote acts like a "controller" for another protocol 
used in deciding some criteria (such as if the contract should be updated in our case) 

There is a "Vote collection box" (or simply a "Vote box" for short), which contains the script described below. It additionally has a NFT that we
call the "controller NFT". This NFT will be used to authenticate the vote box.

There is another token called the "ballot token", where each token corresponds to one vote.
This token will be stored in a "ballot box".


    