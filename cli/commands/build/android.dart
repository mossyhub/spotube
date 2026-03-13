import 'dart:async';
import 'dart:io';

import 'package:args/command_runner.dart';
import 'package:path/path.dart';

import '../../core/env.dart';
import 'common.dart';

class AndroidBuildCommand extends Command with BuildCommandCommonSteps {
  @override
  String get description => "Build for android";

  @override
  String get name => "android";

  @override
  FutureOr? run() async {
    await bootstrap();

    await shell.run(
      "flutter build apk --flavor ${CliEnv.channel.name}",
    );

    final ogApkFile = File(
      join(
        "build",
        "app",
        "outputs",
        "flutter-apk",
        "app-${CliEnv.channel.name}-release.apk",
      ),
    );

    await ogApkFile.copy(
      join(cwd.path, "build", "Spotube-android-all-arch.apk"),
    );

    stdout.writeln("✅ Built Android APK");

    // Build the Android Automotive OS (AAOS) App Bundle for Google Play
    // Use GITHUB_RUN_NUMBER as the build-number so every CI upload has a
    // unique, ever-increasing versionCode as required by Google Play.
    final buildNumber = CliEnv.ghRunNumber ?? '1';
    await shell.run(
      "flutter build appbundle --flavor automotive --build-number $buildNumber",
    );

    final ogAabFile = File(
      join(
        "build",
        "app",
        "outputs",
        "bundle",
        "automotiveRelease",
        "app-automotive-release.aab",
      ),
    );

    await ogAabFile.copy(
      join(cwd.path, "build", "Spotube-automotive-all-arch.aab"),
    );

    stdout.writeln("✅ Built Android Automotive OS (AAOS) App Bundle");
  }
}
