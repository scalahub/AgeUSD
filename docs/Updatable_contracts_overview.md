# Updatable Contracts

Many times tokens are bound to a particular contract, as in the AgeUSD StableCoin protocol. 
There may be times when we want to change the contract subject to certain restrictions. 

We call this design pattern *Updatable Contracts*.

There are various notions of updating contracts, which we will discuss below.

## Update output script
 
Given a box with the script of the form:

    { 
      val outBytes = fromBase64("abcd")
      ...
      someCondition && OUTPUTS(0).propositionBytes = outBytes 
    }
    
    
We want to change the value of `outBytes`. We don't care about preserving Box Id. We may or may not want to preserve registers or tokens

## Update input script

Given a box with the script of the form:

    { 
      someCondition 
    }

We want to change `someCondition`. We don't care about preserving Box Id. We may or may not want to preserve registers or tokens. 

## Update input script

Given a box contract with the script of the form:

    { 
      someCondition 
    }

We want to change `someCondition` and **preserve** the boxId, any registers, and tokens in the box. 

This is the case we will be looking at. Hence, the update should happen without spending the box.  

Let the above box be `B`. Instead of directly storing `someCondition` in `B`, we will first create another box called `C` which contains this code.
Let the hash of the script in `C` be `hash` 

We will also create a new "data" box `D` with `hash` in register R4 and a singleton token with id `dataToken`.

The script of `B` is now modified to be:

    {   
       val correctDataBox = CONTEXT.dataInputs.tokens(0)._1 == dataToken &&
       val hash = CONTEXT.dataInputs.R4[Coll[Byte]].get
       val input = INPUTS(0)
       input.propositionBytes == hash       
    }  

In order to update the script, we need to update the `R4` of the data input box only.
 



    
     
 