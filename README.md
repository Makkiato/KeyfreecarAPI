# KeyfreecarAPI

      프로젝트 필요

기존의 키프리카에서 대부분의 Class와 그 Instance 간 전달을 Broadcast와 global을 이용해 해결하는 문제로 콜스택을 따라가기가 버거운 문제로 유지보수가 불편했다.
또한, Bluetooth peripheral에서 계속 전송되는 정보를 아무런 필터링 없이 Broadcast를 해서 UI 및 인터넷에 전달하니, 배터리 및 데이터 요금제 소모를 비롯한 부분에서도 문제가 완전히 심각했던 적이 있었다.

따라서, 이 프로젝트의 목표는 Broadcast의 남용을 방지하고, 현재 사용자의 UI Context에 맞추어서 Callback을 조정가능한 키프리카 전용의 Bluetooth pripheral manager를 구성하는 것이다.


      프로젝트 구성
      
KeyfreecarAPI 본체와 이를 직접 적용해보기 위한 Test application으로 구성되어있다.
Test application은 논하지 않기로 하고 지금까지 완성된 KeyfreecarAPI에 대해서만 이야기한다.
https://github.com/Makkiato/KeyfreecarAPI/tree/main/API/src/main/java/kr/co/keyfreecar/api
소스코드 파일은 총 4개이고, 거기에 더해 kr.co.keyfreecar.util 패키지에 Consumer lambda inteface를 API 24 이하에서 사용하기 위한 추가적인 파일이 있다.

1. KeyfreecarBluetoothManager.java
사용자가 초기에 이 시스템을 준비할 수 있도록 도와주는 이 API 전체의 Builder 역할을 한다. Bluetooth 장치 검색을 하고, 장치 검색 도중에 발생하는 몇가지 이벤트들에 대한 callback을 제공한다.
이후 사용자는 callback을 통해 받은 검색 결과 중에서 원하는 키프리카 peripheral에 연결을 하여 최종적으로 KeyfreecarConnector instance를 획득할 수 있게 도와준다.

2. KeyfreecarGATT.java
KeyfreecarBluetoothManager를 통해 연결 요청이 들어온 장치에 대한 GATT 연결 준비를 진행해준다. 사용자들에게는 노출되지 않으며, 키프리카 peripheral 내부에서의 비밀번호 인증까지 통과가 되면, KeyfreecarConnector Instance를 생성하고, 이를 onAvailable callback으로 되돌려준다.

3. KeyfreecarConnector.java
키프리카 peripheral에 연결이 완료된 상태에서 취득할 수 있다. 실질적으로 사용자가 global로 두고서 사용하게될 instance라고 염두하고 만들었다.
또한 키프리카 peripheral 하드웨어 스펙과, 이제 실제 차량에까지 연결되는 것을 고려하여(잠금 명령을 과하게 호출시켜, 잠금장치를 불필요하게 작동시켜 노후화), 동일한 명령이 중복으로 여러번 전송되지 않도록, 명령별로 Lock을 걸었다.

4. KeyfreecarDetail.java
KeyfreecarConnector instance로 부터 접근 가능한 장치의 상태와 내부 설정값을 기록하는 class이다. KeyfreecarConnector.setOnUpdate()를 통해 등록된 callback으로 상태 변화에 대한 정보를 전달하고, callback이 등록되지 않았어도 getter를 통해 취득할수 있다.
