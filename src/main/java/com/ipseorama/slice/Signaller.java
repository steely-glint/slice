/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice;

/**
 *
 * @author tim
 */
interface Signaller {
    void candidateChangeEvent(ICECandidate candidate);
}
