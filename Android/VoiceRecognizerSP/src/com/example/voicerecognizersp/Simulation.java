package com.example.voicerecognizersp;

/**
 * This class maintains all necessary variables which are required for simulating certain defined scenarios.
 * It will also help in testing and debugging working of application in different scenarios
 * 
 * @author Wahib-Ul-Haq 
 * Apr 6, 2015
 *
 */
public class Simulation {
	
	public static boolean SWITCH_FORWARD_FROM_IDLE_TO_SPEECH;
	public static boolean SWITCH_FORWARD_FROM_SPEECH_TO_SPEAKER;
	public static boolean SWITCH_BACKWARD_FROM_SPEAKER_TO_SPEECH;
	public static boolean SWITCH_BACKWARD_FROM_SPEECH_TO_IDLE;
	
	public static boolean REMAIN_IN_IDLE;
	public static boolean REMAIN_IN_SPEECH;
	public static boolean REMAIN_IN_SPEAKER;

	public Simulation() {
		
	}
	
	
	/**
	 * Start with Idle -> remain in Idle
	 * 
	 */
	public void scenario1() {
		SWITCH_FORWARD_FROM_IDLE_TO_SPEECH = false;
		REMAIN_IN_IDLE = true;
	}
	
	/**
	 * Start with Idle -> Switch to Speech -> remain in Speech 
	 */
	public void scenario2() {
		SWITCH_FORWARD_FROM_IDLE_TO_SPEECH = true;
		REMAIN_IN_SPEECH = true;
	}
	
	/**
	 * Start with Idle -> Switch to Speech -> Switch to Speaker -> remain in Speaker 
	 */
	public void scenario3() {
		SWITCH_FORWARD_FROM_IDLE_TO_SPEECH = true;
		SWITCH_FORWARD_FROM_SPEECH_TO_SPEAKER = true;
		REMAIN_IN_SPEAKER = true;
		
	}
	
	/**
	 * Start with Idle --> Switch to Speech --> Switch to Speaker --> Switch back to Speech --> remain in Speech
	 */
	public void scenario4() {
		SWITCH_FORWARD_FROM_IDLE_TO_SPEECH = true;
		SWITCH_FORWARD_FROM_SPEECH_TO_SPEAKER = true;
		SWITCH_BACKWARD_FROM_SPEAKER_TO_SPEECH = true;
		REMAIN_IN_SPEECH = true;
		
	}
	
	/**
	 * Start with Idle --> Switch to Speech --> Switch to Speaker --> Switch back to Speech --> switch back to Idle --> remain in Idle
	 */
	public void scenario5() {
		SWITCH_FORWARD_FROM_IDLE_TO_SPEECH = true;
		SWITCH_FORWARD_FROM_SPEECH_TO_SPEAKER = true;
		SWITCH_BACKWARD_FROM_SPEAKER_TO_SPEECH = true;
		SWITCH_BACKWARD_FROM_SPEECH_TO_IDLE = true;
		REMAIN_IN_IDLE = true;
	}
	
	/**
	 * Keep scenario5 on repeat
	 */
	public void scenario6() {
		
	}
	
	
}
