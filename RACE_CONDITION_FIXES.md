# Authentication Race Condition Fixes

## Problem Summary
The Cerberus application was experiencing race conditions and multiple authentication prompts being displayed simultaneously, leading to inconsistent behavior and poor user experience.

## Root Causes Identified

1. **BiometricPromptActivity Race Conditions**: The `promptShowing` flag and retriggering logic could create inconsistent states
2. **No Activity Instance Management**: Multiple authenticator prompt activities could launch simultaneously 
3. **Incomplete Dismiss Handling**: Missing AUTH_DISMISSED broadcast handling for proper cleanup
4. **Broadcast Receiver Timing Issues**: Inconsistent receiver management across authenticators

## Solution Implemented

### 1. PromptActivityManager Singleton
- **File**: `app/src/main/java/com/example/cerberus/auth/PromptActivityManager.kt`
- **Purpose**: Centralized coordination to prevent multiple prompts per package
- **Key Features**:
  - Thread-safe registration/unregistration of prompt activities
  - Per-package prompt coordination
  - Provides synchronization locks for race condition prevention
  - Support for multiple packages with different prompt types

### 2. Enhanced Prompt Activities
**Files Modified**:
- `BiometricPromptActivity.kt`
- `PinPromptActivity.kt` 
- `PatternPromptActivity.kt`
- `PasswordPromptActivity.kt`

**Changes Made**:
- Register with PromptActivityManager on creation to prevent duplicates
- Send AUTH_DISMISSED broadcast when back button pressed or activity dismissed
- Proper cleanup in onDestroy methods
- Race condition prevention using synchronized blocks (BiometricPromptActivity)

### 3. Updated Authenticators
**Files Modified**:
- `BiometricAuthenticator.kt`
- `PinAuthenticator.kt`
- `PatternAuthenticator.kt` 
- `PasswordAuthenticator.kt`

**Changes Made**:
- Listen for AUTH_DISMISSED broadcasts
- Treat dismissal as authentication failure for proper cleanup
- Consistent broadcast receiver management

## How Race Conditions Are Prevented

### Before Fix:
```kotlin
// BiometricPromptActivity - Race condition possible
private fun retriggerPrompt() {
    if (!promptShowing) {  // Thread A checks false
        // Thread B could set promptShowing = true here
        handler.post { showBiometricPrompt() }  // Thread A proceeds anyway
    }
}
```

### After Fix:
```kotlin
// BiometricPromptActivity - Race condition prevented
@Volatile private var promptShowing = false
private fun retriggerPrompt() {
    val lock = PromptActivityManager.getPromptLock(packageNameToAuth!!)
    if (lock != null) {
        synchronized(lock) {  // Only one thread can execute this block
            if (!promptShowing && !isFinishing) {
                handler.post { showBiometricPrompt() }
            }
        }
    }
}
```

### Multiple Prompt Prevention:
```kotlin
// PromptActivityManager coordination
@Synchronized
fun registerPrompt(packageName: String, promptType: String): Boolean {
    val existingPrompt = activePrompts[packageName]
    if (existingPrompt != null) {
        return false  // Reject if another prompt is active
    }
    activePrompts[packageName] = promptType
    return true
}
```

## Authentication Flow After Fix

1. **Authenticator** calls `authenticate(context, packageName)`
2. **PromptActivity** attempts to register with PromptActivityManager
3. If another prompt is active for the same package → **Activity finishes immediately**
4. If registration succeeds → **Prompt shows normally**
5. User interaction:
   - **Success** → AUTH_SUCCESS broadcast → Authentication succeeds
   - **Failure** → AUTH_FAILURE broadcast → Authentication fails  
   - **Dismiss/Back** → AUTH_DISMISSED broadcast → Authentication fails
6. **Activity cleanup** → Unregister from PromptActivityManager

## Testing

### Unit Tests Added:
- `PromptActivityManagerTest.kt`: Comprehensive testing of coordination logic
- `AuthenticatorIntegrationTest.kt`: Basic integration testing of authenticator behavior

### Test Coverage:
- Single prompt registration
- Multiple prompt prevention for same package
- Different packages can have simultaneous prompts
- Proper unregistration and cleanup
- Callback management in authenticators

## Expected Behavior After Fix

✅ **Only one authentication prompt per package at a time**  
✅ **Proper handling of prompt dismissal with AUTH_DISMISSED broadcast**  
✅ **No race conditions in prompt showing/hiding**  
✅ **Clean return to home when authentication is dismissed**  
✅ **Consistent authentication state management across all authenticator types**

## Minimal Change Impact

The solution maintains the existing architecture (applock → authservice → authenticator → prompt activity) while adding coordination and synchronization. No major structural changes were made, ensuring compatibility and minimizing risk.

**Change Summary**:
- 1 new file (PromptActivityManager)
- 8 modified files (4 prompt activities + 4 authenticators)  
- Enhanced synchronization and coordination
- Consistent AUTH_DISMISSED handling
- Comprehensive test coverage