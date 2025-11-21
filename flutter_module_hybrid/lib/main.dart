import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: Scaffold(
        body: Center(
          child: Text(
            '¡Hola Mundo desde Flutter. Si ves esto, ya está funcando!!!! Lo que me costó jajaj',
            style: TextStyle(fontSize: 28),
          ),
        ),
      ),
    );
  }
}
