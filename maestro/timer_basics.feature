Feature: Timer Basics
  As a user
  I want to create, start, pause, and clear timers
  So that I can manage my tasks efficiently

  Scenario: Creating and interacting with a manual timer
    Given the app is launched
    When I tap on "Add Timer"
    Then I should see "Sets:"
    When I tap on "1"
    And I tap on "0"
    And I tap on "0"
    And I tap on "Start"
    Then I should see "Set 1"
    When I wait until "00:58" is visible
    And I tap on "Play/Pause"
    And I tap on "Clear"
    Then I should not see "Set 1"
